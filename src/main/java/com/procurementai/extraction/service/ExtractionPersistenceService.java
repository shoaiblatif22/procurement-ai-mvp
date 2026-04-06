package com.procurementai.extraction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procurementai.extraction.service.dto.ExtractedLineItem;
import com.procurementai.extraction.service.dto.QuoteExtractionResult;
import com.procurementai.extraction.model.ExtractionStatus;
import com.procurementai.document.model.Document;
import com.procurementai.document.model.DocumentStatus;
import com.procurementai.document.model.ExtractionJob;
import com.procurementai.document.repository.DocumentRepository;
import com.procurementai.document.repository.ExtractionJobRepository;
import com.procurementai.quote.model.Quote;
import com.procurementai.quote.model.QuoteLineItem;
import com.procurementai.quote.repository.QuoteRepository;
import com.procurementai.quote.repository.QuoteLineItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Persists extraction results into the quotes + quote_line_items tables
 * and updates the document/extraction_job status accordingly.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExtractionPersistenceService {

    private final DocumentRepository      documentRepository;
    private final QuoteRepository         quoteRepository;
    private final QuoteLineItemRepository quoteLineItemRepository;
    private final ExtractionJobRepository extractionJobRepository;
    private final ObjectMapper            objectMapper;

    @Transactional
    public Quote persistExtractionResult(UUID documentId, QuoteExtractionResult result) {

        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        ExtractionJob job = extractionJobRepository.findByDocumentId(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Extraction job not found for document: " + documentId));

        // Mark job in-progress
        job.setStatus(ExtractionStatus.IN_PROGRESS);
        job.setStartedAt(OffsetDateTime.now());
        extractionJobRepository.save(job);

        try {
            // Build Quote entity
            Quote quote = Quote.builder()
                .document(document)
                .company(document.getCompany())
                .supplier(document.getSupplier())
                .supplierNameRaw(result.supplierName())
                .quoteReference(result.quoteReference())
                .quoteDate(parseDate(result.quoteDate()))
                .validUntil(parseDate(result.validUntil()))
                .currency(result.currency() != null ? result.currency() : "GBP")
                .subtotal(result.subtotal())
                .taxAmount(result.taxAmount())
                .totalAmount(result.totalAmount())
                .paymentTerms(result.paymentTerms())
                .deliveryTerms(result.deliveryTerms())
                .leadTimeDays(result.leadTimeDays())
                .notes(result.notes())
                .extractionStatus(ExtractionStatus.COMPLETED)
                .extractionConfidence(result.overallConfidence())
                .requiresReview(result.requiresReview())
                .rawExtraction(result.rawResponse())
                .build();

            quote = quoteRepository.save(quote);

            // Persist line items
            if (result.lineItems() != null) {
                for (ExtractedLineItem item : result.lineItems()) {
                    String flaggedJson = toJson(item.flaggedFields());
                    QuoteLineItem lineItem = QuoteLineItem.builder()
                        .quote(quote)
                        .lineNumber(item.lineNumber())
                        .description(item.description())
                        .quantity(item.quantity())
                        .unit(item.unit())
                        .unitPrice(item.unitPrice())
                        .discountPct(item.discountPct())
                        .lineTotal(item.lineTotal())
                        .sku(item.sku())
                        .confidenceDescription(item.confidenceDescription())
                        .confidenceQuantity(item.confidenceQuantity())
                        .confidenceUnitPrice(item.confidenceUnitPrice())
                        .confidenceLineTotal(item.confidenceLineTotal())
                        .flaggedFields(flaggedJson)
                        .build();
                    quoteLineItemRepository.save(lineItem);
                }
            }

            // Mark document and job as completed
            document.setStatus(DocumentStatus.COMPLETED);
            documentRepository.save(document);

            job.setStatus(ExtractionStatus.COMPLETED);
            job.setCompletedAt(OffsetDateTime.now());
            extractionJobRepository.save(job);

            log.info("Persisted extraction for document {} — quote {}, {} line items, confidence {}",
                documentId, quote.getId(),
                result.lineItems() != null ? result.lineItems().size() : 0,
                result.overallConfidence());

            return quote;

        } catch (Exception e) {
            log.error("Failed to persist extraction for document {}: {}", documentId, e.getMessage());

            document.setStatus(DocumentStatus.FAILED);
            document.setProcessingError(e.getMessage());
            documentRepository.save(document);

            job.setStatus(ExtractionStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setAttempts(job.getAttempts() + 1);
            extractionJobRepository.save(job);

            throw e;
        }
    }

    @Transactional
    public void markFailed(UUID documentId, String errorMessage) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setStatus(DocumentStatus.FAILED);
            doc.setProcessingError(errorMessage);
            documentRepository.save(doc);
        });
        extractionJobRepository.findByDocumentId(documentId).ifPresent(job -> {
            job.setStatus(ExtractionStatus.FAILED);
            job.setErrorMessage(errorMessage);
            job.setAttempts(job.getAttempts() + 1);
            extractionJobRepository.save(job);
        });
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            log.warn("Could not parse date '{}', skipping", dateStr);
            return null;
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}
