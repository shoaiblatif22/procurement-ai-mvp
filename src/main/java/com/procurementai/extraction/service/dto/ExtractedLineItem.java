package com.procurementai.extraction.service.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExtractedLineItem(
    int lineNumber,
    String description,
    BigDecimal quantity,
    String unit,
    BigDecimal unitPrice,
    BigDecimal discountPct,
    BigDecimal lineTotal,
    String sku,
    BigDecimal confidenceDescription,
    BigDecimal confidenceQuantity,
    BigDecimal confidenceUnitPrice,
    BigDecimal confidenceLineTotal,
    List<String> flaggedFields
) {}
