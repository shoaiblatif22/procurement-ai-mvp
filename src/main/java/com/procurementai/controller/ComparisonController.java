package com.procurementai.controller;

import com.procurementai.model.*;
import com.procurementai.repository.*;
import com.procurementai.service.ComparisonService;
import com.procurementai.service.ComparisonMatrix;
import com.procurementai.service.QuoteSnapshot;
import com.procurementai.service.LineItemSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Comparison creation and analysis endpoints.
 *
 * POST /api/v1/comparisons               — create new comparison
 * POST /api/v1/comparisons/{id}/quotes   — add quote to comparison
 * GET  /api/v1/comparisons/{id}/matrix   — get full side-by-side matrix
 * GET  /api/v1/comparisons/{id}/summary  — get AI summary + recommendation
 */
@RestController
@RequestMapping("/api/v1/comparisons")
@RequiredArgsConstructor
@Slf4j
public class ComparisonController {

    private static final UUID DEMO_COMPANY_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final UUID DEMO_USER_ID    = UUID.fromString("b0000000-0000-0000-0000-000000000001");

    private final ComparisonRepository      comparisonRepository;
    private final ComparisonQuoteRepository comparisonQuoteRepository;
    private final QuoteRepository           quoteRepository;
    private final QuoteLineItemRepository   quoteLineItemRepository;
    private final CompanyRepository         companyRepository;
    private final UserRepository            userRepository;
    private final ComparisonService         comparisonService;

    @PostMapping
    public ResponseEntity<ComparisonCreatedResponse> createComparison(
            @RequestBody CreateComparisonRequest request) {

        Company company = companyRepository.findById(DEMO_COMPANY_ID)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Demo company not found"));
        User creator = userRepository.findById(DEMO_USER_ID)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Demo user not found"));

        Comparison comparison = Comparison.builder()
            .company(company)
            .createdBy(creator)
            .title(request.title())
            .description(request.description())
            .category(request.category())
            .status("DRAFT")
            .build();

        comparison = comparisonRepository.save(comparison);
        log.info("Created comparison {} — '{}'", comparison.getId(), comparison.getTitle());

        return ResponseEntity.status(HttpStatus.CREATED).body(new ComparisonCreatedResponse(
            comparison.getId(),
            comparison.getTitle(),
            comparison.getStatus(),
            "/api/v1/comparisons/" + comparison.getId()
        ));
    }

    @PostMapping("/{comparisonId}/quotes")
    public ResponseEntity<Void> addQuoteToComparison(
            @PathVariable UUID comparisonId,
            @RequestBody AddQuoteRequest request) {

        Comparison comparison = comparisonRepository.findById(comparisonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comparison not found"));

        Quote quote = quoteRepository.findById(request.quoteId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote not found"));

        if (comparisonQuoteRepository.existsByComparisonIdAndQuoteId(comparisonId, request.quoteId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        int sortOrder = comparisonQuoteRepository
            .findByComparisonIdOrderBySortOrderAsc(comparisonId).size();

        ComparisonQuote cq = ComparisonQuote.builder()
            .comparison(comparison)
            .quote(quote)
            .sortOrder(sortOrder)
            .build();

        comparisonQuoteRepository.save(cq);
        log.info("Added quote {} to comparison {}", request.quoteId(), comparisonId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{comparisonId}/matrix")
    public ResponseEntity<ComparisonMatrixResponse> getMatrix(
            @PathVariable UUID comparisonId) {

        Comparison comparison = comparisonRepository.findById(comparisonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comparison not found"));

        List<ComparisonQuote> cqs = comparisonQuoteRepository
            .findByComparisonIdOrderBySortOrderAsc(comparisonId);

        if (cqs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "No quotes added to this comparison yet");
        }

        List<QuoteSnapshot> snapshots = cqs.stream()
            .map(cq -> toSnapshot(cq.getQuote()))
            .collect(Collectors.toList());

        ComparisonMatrix matrix = comparisonService.buildMatrix(snapshots);

        List<QuoteRow> quoteRows = matrix.quotes().stream()
            .map(q -> new QuoteRow(
                q.quoteId(), q.supplierName(), q.quoteReference(),
                q.subtotal() != null ? q.subtotal().doubleValue() : 0,
                q.taxAmount() != null ? q.taxAmount().doubleValue() : 0,
                q.totalAmount() != null ? q.totalAmount().doubleValue() : 0,
                q.paymentTerms(), q.deliveryTerms(),
                q.leadTimeDays() != null ? q.leadTimeDays() : 0,
                q.extractionConfidence() != null ? q.extractionConfidence().doubleValue() : 0,
                q.requiresReview()
            ))
            .toList();

        List<LineMatchRow> lineMatchRows = matrix.matchedGroups().stream()
            .map(g -> new LineMatchRow(
                g.normalisedDescription(),
                g.itemsBySupplier().stream()
                    .map(item -> new SupplierLineItem(
                        item.supplierName(),
                        item.lineItem().quantity() != null ? item.lineItem().quantity().doubleValue() : null,
                        item.lineItem().unit(),
                        item.lineItem().unitPrice() != null ? item.lineItem().unitPrice().doubleValue() : null,
                        item.lineItem().lineTotal() != null ? item.lineItem().lineTotal().doubleValue() : null,
                        item.lineItem().confidenceUnitPrice() != null ? item.lineItem().confidenceUnitPrice().doubleValue() : 0,
                        item.lineItem().flaggedFields()
                    ))
                    .toList(),
                g.cheapest() != null ? g.cheapest().supplierName() : null,
                g.cheapest() != null && g.mostExpensive() != null
                    ? g.mostExpensive().lineItem().lineTotal().subtract(g.cheapest().lineItem().lineTotal()).doubleValue()
                    : null
            ))
            .toList();

        ComparisonSummaryRow summaryRow = new ComparisonSummaryRow(
            matrix.summary().quoteCount(),
            matrix.summary().cheapestQuote() != null ? matrix.summary().cheapestQuote().supplierName() : null,
            matrix.summary().cheapestQuote() != null && matrix.summary().cheapestQuote().totalAmount() != null
                ? matrix.summary().cheapestQuote().totalAmount().doubleValue() : 0,
            matrix.summary().mostExpensiveQuote() != null ? matrix.summary().mostExpensiveQuote().supplierName() : null,
            matrix.summary().mostExpensiveQuote() != null && matrix.summary().mostExpensiveQuote().totalAmount() != null
                ? matrix.summary().mostExpensiveQuote().totalAmount().doubleValue() : 0,
            matrix.summary().potentialSaving().doubleValue(),
            matrix.summary().missingItemCount()
        );

        return ResponseEntity.ok(new ComparisonMatrixResponse(
            comparisonId, comparison.getTitle(), quoteRows, lineMatchRows, summaryRow
        ));
    }

    @GetMapping("/{comparisonId}/summary")
    public ResponseEntity<AiSummaryResponse> getAiSummary(
            @PathVariable UUID comparisonId) {

        comparisonRepository.findById(comparisonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comparison not found"));

        List<ComparisonQuote> cqs = comparisonQuoteRepository
            .findByComparisonIdOrderBySortOrderAsc(comparisonId);

        if (cqs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "No quotes added to this comparison yet");
        }

        List<QuoteSnapshot> snapshots = cqs.stream()
            .map(cq -> toSnapshot(cq.getQuote()))
            .collect(Collectors.toList());

        ComparisonMatrix matrix = comparisonService.buildMatrix(snapshots);

        // Block for the Claude response (acceptable in a demo/non-streaming endpoint)
        var aiSummary = comparisonService.generateAiSummary(matrix).block();

        return ResponseEntity.ok(new AiSummaryResponse(
            aiSummary != null ? aiSummary.rawResponse() : "No summary available",
            null,
            List.of()
        ));
    }

    // ── Mapping helpers ────────────────────────────────────────

    private QuoteSnapshot toSnapshot(Quote quote) {
        List<QuoteLineItem> items = quoteLineItemRepository
            .findByQuoteIdOrderByLineNumberAsc(quote.getId());

        List<LineItemSnapshot> lineSnapshots = items.stream()
            .map(li -> new LineItemSnapshot(
                li.getId(),
                li.getLineNumber(),
                li.getDescription(),
                li.getQuantity(),
                li.getUnit(),
                li.getUnitPrice(),
                li.getLineTotal(),
                li.getConfidenceUnitPrice(),
                List.of() // flaggedFields stored as JSON string — skip for now
            ))
            .toList();

        return new QuoteSnapshot(
            quote.getId(),
            quote.getSupplier() != null ? quote.getSupplier().getId() : quote.getId(),
            quote.getSupplier() != null ? quote.getSupplier().getName() : quote.getSupplierNameRaw(),
            quote.getQuoteReference(),
            quote.getSubtotal(),
            quote.getTaxAmount(),
            quote.getTotalAmount(),
            quote.getPaymentTerms(),
            quote.getDeliveryTerms(),
            quote.getLeadTimeDays(),
            quote.getExtractionConfidence(),
            quote.isRequiresReview(),
            lineSnapshots
        );
    }
}

record CreateComparisonRequest(String title, String description, String category) {}
record ComparisonCreatedResponse(UUID id, String title, String status, String url) {}
record AddQuoteRequest(UUID quoteId) {}

record QuoteRow(UUID quoteId, String supplierName, String reference,
                double subtotal, double taxAmount, double totalAmount,
                String paymentTerms, String deliveryTerms, int leadTimeDays,
                double confidence, boolean requiresReview) {}

record LineMatchRow(String description, List<SupplierLineItem> bySupplier,
                    String cheapestSupplier, Double saving) {}

record SupplierLineItem(String supplierName, Double quantity, String unit,
                        Double unitPrice, Double lineTotal, double confidence,
                        List<String> flaggedFields) {}

record ComparisonSummaryRow(int quoteCount, String cheapestSupplier, double cheapestTotal,
                             String mostExpensiveSupplier, double mostExpensiveTotal,
                             double potentialSaving, int missingItemCount) {}

record ComparisonMatrixResponse(UUID comparisonId, String title, List<QuoteRow> quotes,
                                 List<LineMatchRow> lineMatches, ComparisonSummaryRow summary) {}

record AiSummaryResponse(String summary, String recommendation, List<String> watchOutFor) {}
