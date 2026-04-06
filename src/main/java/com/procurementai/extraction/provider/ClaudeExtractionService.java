package com.procurementai.extraction.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.procurementai.extraction.service.ExtractionParseException;
import com.procurementai.extraction.service.QuoteExtractionService;
import com.procurementai.extraction.service.dto.ExtractedLineItem;
import com.procurementai.extraction.service.dto.QuoteExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Calls the Claude API to extract structured data from raw OCR text.
 */
// @Service  // Commented out — using OllamaExtractionService instead
@Slf4j
@RequiredArgsConstructor
public class ClaudeExtractionService implements QuoteExtractionService {

    private final WebClient claudeWebClient;
    private final ObjectMapper objectMapper;

    @Value("${app.claude.model}")
    private String model;

    @Value("${app.claude.max-tokens}")
    private int maxTokens;

    @Value("${app.extraction.confidence-threshold}")
    private BigDecimal confidenceThreshold;

    // ── Main extraction method ─────────────────────────────────

    @Override
    public Mono<QuoteExtractionResult> extractQuoteData(String ocrText, String originalFilename) {
        String prompt = buildExtractionPrompt(ocrText, originalFilename);

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "system", SYSTEM_PROMPT,
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            )
        );

        log.debug("Sending extraction request to Claude for document: {}", originalFilename);

        return claudeWebClient.post()
            .uri("/messages")
            .bodyValue(requestBody)
            .retrieve()
            .onStatus(status -> status.isError(), response ->
                response.bodyToMono(String.class)
                    .flatMap(body -> {
                        log.error("Claude API error {} for {}: {}", response.statusCode(), originalFilename, body);
                        return Mono.error(new RuntimeException("Claude API " + response.statusCode() + ": " + body));
                    })
            )
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(60))
            .map(this::parseClaudeResponse)
            .doOnSuccess(result -> log.info(
                "Extraction complete for {}. Confidence: {}, Items: {}, Requires review: {}",
                originalFilename,
                result.overallConfidence(),
                result.lineItems().size(),
                result.requiresReview()
            ))
            .doOnError(e -> log.error("Claude extraction failed for {}: {}", originalFilename, e.getMessage()));
    }

    // ── Response parsing ───────────────────────────────────────

    private QuoteExtractionResult parseClaudeResponse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String content = root.path("content").get(0).path("text").asText();

            // Strip any markdown code fences Claude might add
            content = content.replaceAll("```json\\n?", "").replaceAll("```\\n?", "").trim();

            JsonNode extracted = objectMapper.readTree(content);
            return mapToExtractionResult(extracted, rawResponse);

        } catch (Exception e) {
            log.error("Failed to parse Claude response: {}", e.getMessage());
            throw new ExtractionParseException("Failed to parse AI extraction response", e);
        }
    }

    private QuoteExtractionResult mapToExtractionResult(JsonNode node, String rawResponse) {
        List<ExtractedLineItem> lineItems = objectMapper.convertValue(
            node.path("line_items"),
            objectMapper.getTypeFactory().constructCollectionType(List.class, ExtractedLineItem.class)
        );

        BigDecimal confidence = node.path("overall_confidence").decimalValue();
        boolean requiresReview = confidence.compareTo(confidenceThreshold) < 0;

        return new QuoteExtractionResult(
            node.path("supplier_name").asText(null),
            node.path("quote_reference").asText(null),
            node.path("quote_date").asText(null),
            node.path("valid_until").asText(null),
            node.path("currency").asText("GBP"),
            node.path("subtotal").decimalValue(),
            node.path("tax_amount").decimalValue(),
            node.path("total_amount").decimalValue(),
            node.path("payment_terms").asText(null),
            node.path("delivery_terms").asText(null),
            node.path("lead_time_days").isNull() ? null : node.path("lead_time_days").intValue(),
            node.path("notes").asText(null),
            lineItems,
            confidence,
            requiresReview,
            node.path("missing_fields").toString(),
            rawResponse
        );
    }

    // ── Prompt engineering ─────────────────────────────────────

    private String buildExtractionPrompt(String ocrText, String filename) {
        return """
            Extract all procurement quote data from the following document text.

            Document filename: %s

            Document text:
            ---
            %s
            ---

            Return ONLY a JSON object. No markdown, no explanation, just the JSON.
            """.formatted(filename, ocrText);
    }

    // ── System prompt ─────────────────────────────────────────

    private static final String SYSTEM_PROMPT = """
        You are a precision procurement data extraction engine. Your job is to extract
        structured data from supplier quote documents with high accuracy.

        Always respond with ONLY valid JSON matching this exact schema:

        {
          "supplier_name": "string or null",
          "quote_reference": "string or null",
          "quote_date": "YYYY-MM-DD or null",
          "valid_until": "YYYY-MM-DD or null",
          "currency": "GBP",
          "subtotal": number or null,
          "tax_amount": number or null,
          "total_amount": number or null,
          "payment_terms": "string or null",
          "delivery_terms": "string or null",
          "lead_time_days": integer or null,
          "notes": "string or null",
          "line_items": [
            {
              "line_number": integer,
              "description": "string",
              "quantity": number or null,
              "unit": "string or null",
              "unit_price": number or null,
              "discount_pct": number or null,
              "line_total": number or null,
              "sku": "string or null",
              "confidence_description": 0.0-1.0,
              "confidence_quantity": 0.0-1.0,
              "confidence_unit_price": 0.0-1.0,
              "confidence_line_total": 0.0-1.0,
              "flagged_fields": ["field_name"] or []
            }
          ],
          "overall_confidence": 0.0-1.0,
          "missing_fields": ["field_name"] or []
        }

        Confidence scoring rules:
        - 1.0: Value is clearly present and unambiguous
        - 0.8-0.9: Value extracted with minor uncertainty (formatting, abbreviation)
        - 0.6-0.7: Value inferred or partially visible
        - Below 0.6: Add field to flagged_fields

        Critical rules:
        - All monetary values must be numbers (no currency symbols, no commas)
        - Dates must be YYYY-MM-DD format
        - If a value is truly absent, use null — do not guess
        - Lead time: convert to days (e.g. "2 weeks" = 14)
        - If multiple VAT rates appear, extract at line item level
        - overall_confidence = weighted average of all field confidences
        """;
}
