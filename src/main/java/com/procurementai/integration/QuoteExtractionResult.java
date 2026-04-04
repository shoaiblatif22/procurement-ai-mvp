package com.procurementai.integration;

import java.math.BigDecimal;
import java.util.List;

public record QuoteExtractionResult(
    String supplierName,
    String quoteReference,
    String quoteDate,
    String validUntil,
    String currency,
    BigDecimal subtotal,
    BigDecimal taxAmount,
    BigDecimal totalAmount,
    String paymentTerms,
    String deliveryTerms,
    Integer leadTimeDays,
    String notes,
    List<ExtractedLineItem> lineItems,
    BigDecimal overallConfidence,
    boolean requiresReview,
    String missingFields,
    String rawResponse
) {}
