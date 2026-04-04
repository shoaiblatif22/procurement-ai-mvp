package com.procurementai.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record QuoteSnapshot(
    UUID quoteId,
    UUID supplierId,
    String supplierName,
    String quoteReference,
    BigDecimal subtotal,
    BigDecimal taxAmount,
    BigDecimal totalAmount,
    String paymentTerms,
    String deliveryTerms,
    Integer leadTimeDays,
    BigDecimal extractionConfidence,
    boolean requiresReview,
    List<LineItemSnapshot> lineItems
) {}
