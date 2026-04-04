package com.procurementai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Generates side-by-side comparisons from multiple extracted quotes.
 * This is the core product feature users see in the demo.
 *
 * Also calls Gemini to generate a plain-English summary and recommendation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ComparisonService {

    // private final WebClient claudeWebClient;  // Commented out — using Gemini
    private final WebClient geminiWebClient;

    // @Value("${app.claude.model}")
    // private String model;
    @Value("${app.gemini.model}")
    private String model;

    // ── Build a comparison matrix ──────────────────────────────

    public ComparisonMatrix buildMatrix(List<QuoteSnapshot> quotes) {
        if (quotes.isEmpty()) throw new IllegalArgumentException("Need at least one quote");
        if (quotes.size() > 10) throw new IllegalArgumentException("Maximum 10 quotes per comparison");

        List<MatchedLineGroup> matchedGroups = matchLineItems(quotes);
        ComparisonSummary summary = calculateSummary(quotes, matchedGroups);

        return new ComparisonMatrix(quotes, matchedGroups, summary);
    }

    // ── Gemini AI summary ──────────────────────────────────────

    public Mono<AiComparisonSummary> generateAiSummary(ComparisonMatrix matrix) {
        String matrixText = formatMatrixForPrompt(matrix);
        String prompt = SUMMARY_SYSTEM_PROMPT + "\n\n" + matrixText;

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            ),
            "generationConfig", Map.of(
                "temperature", 0.1,
                "maxOutputTokens", 1000,
                "responseMimeType", "application/json"
            )
        );

        return geminiWebClient.post()
            .uri("/v1beta/models/{model}:generateContent", model)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .map(this::parseAiSummaryResponse);
    }

    // ── Line item matching (fuzzy match across quotes) ─────────

    private List<MatchedLineGroup> matchLineItems(List<QuoteSnapshot> quotes) {
        // Simple implementation for MVP: match by normalised description
        // In production: use embedding similarity or more sophisticated NLP
        Map<String, List<MatchedItem>> groups = new LinkedHashMap<>();

        for (QuoteSnapshot quote : quotes) {
            for (LineItemSnapshot item : quote.lineItems()) {
                String key = normaliseDescription(item.description());
                groups.computeIfAbsent(key, k -> new ArrayList<>())
                      .add(new MatchedItem(quote.supplierId(), quote.supplierName(), item));
            }
        }

        return groups.entrySet().stream()
            .map(entry -> new MatchedLineGroup(
                entry.getKey(),
                entry.getValue(),
                findCheapest(entry.getValue()),
                findMostExpensive(entry.getValue()),
                isMissingFromSomeQuotes(entry.getValue(), quotes.size())
            ))
            .toList();
    }

    private String normaliseDescription(String description) {
        return description.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private MatchedItem findCheapest(List<MatchedItem> items) {
        return items.stream()
            .filter(i -> i.lineItem().lineTotal() != null)
            .min(Comparator.comparing(i -> i.lineItem().lineTotal()))
            .orElse(null);
    }

    private MatchedItem findMostExpensive(List<MatchedItem> items) {
        return items.stream()
            .filter(i -> i.lineItem().lineTotal() != null)
            .max(Comparator.comparing(i -> i.lineItem().lineTotal()))
            .orElse(null);
    }

    private boolean isMissingFromSomeQuotes(List<MatchedItem> items, int totalQuotes) {
        Set<UUID> supplierIds = new HashSet<>();
        items.forEach(i -> supplierIds.add(i.supplierId()));
        return supplierIds.size() < totalQuotes;
    }

    // ── Summary stats ──────────────────────────────────────────

    private ComparisonSummary calculateSummary(
            List<QuoteSnapshot> quotes,
            List<MatchedLineGroup> groups) {

        QuoteSnapshot cheapest = quotes.stream()
            .filter(q -> q.totalAmount() != null)
            .min(Comparator.comparing(QuoteSnapshot::totalAmount))
            .orElse(null);

        QuoteSnapshot mostExpensive = quotes.stream()
            .filter(q -> q.totalAmount() != null)
            .max(Comparator.comparing(QuoteSnapshot::totalAmount))
            .orElse(null);

        BigDecimal potentialSaving = BigDecimal.ZERO;
        if (cheapest != null && mostExpensive != null) {
            potentialSaving = mostExpensive.totalAmount()
                .subtract(cheapest.totalAmount())
                .setScale(2, RoundingMode.HALF_UP);
        }

        long missingItemCount = groups.stream()
            .filter(MatchedLineGroup::missingFromSomeQuotes)
            .count();

        return new ComparisonSummary(
            quotes.size(),
            cheapest,
            mostExpensive,
            potentialSaving,
            (int) missingItemCount
        );
    }

    // ── Format for Gemini prompt ───────────────────────────────

    private String formatMatrixForPrompt(ComparisonMatrix matrix) {
        StringBuilder sb = new StringBuilder();
        sb.append("Supplier quote comparison:\n\n");

        matrix.quotes().forEach(q -> {
            sb.append("SUPPLIER: ").append(q.supplierName()).append("\n");
            sb.append("Total: £").append(q.totalAmount()).append("\n");
            sb.append("Payment terms: ").append(q.paymentTerms()).append("\n");
            sb.append("Lead time: ").append(q.leadTimeDays()).append(" days\n\n");
        });

        sb.append("Potential saving vs most expensive: £")
          .append(matrix.summary().potentialSaving()).append("\n");
        sb.append("Items missing from some quotes: ")
          .append(matrix.summary().missingItemCount()).append("\n");

        return sb.toString();
    }

    private AiComparisonSummary parseAiSummaryResponse(String raw) {
        // Parse the Gemini response — extract text from candidates[0].content.parts[0].text
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var root = mapper.readTree(raw);
            String text = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();
            return new AiComparisonSummary(text, null);
        } catch (Exception e) {
            log.warn("Failed to parse Gemini summary response, returning raw: {}", e.getMessage());
            return new AiComparisonSummary(raw, null);
        }
    }

    private static final String SUMMARY_SYSTEM_PROMPT = """
        You are a procurement advisor helping a buyer compare supplier quotes.
        
        Analyse the comparison data and respond with JSON:
        {
          "summary": "2-3 sentence plain English summary of the key differences",
          "recommendation": "which supplier to choose and why, in 2-3 sentences",
          "watch_out_for": ["list of concerns or missing items the buyer should ask about"]
        }
        
        Be direct and practical. Mention specific numbers.
        Focus on total cost, payment terms, lead time, and any missing items.
        Do not be vague. A procurement manager needs a clear steer.
        """;
}

// DTOs moved to standalone public files in this package
