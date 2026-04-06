package com.procurementai.comparison.service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record LineItemSnapshot(
    UUID lineItemId,
    int lineNumber,
    String description,
    BigDecimal quantity,
    String unit,
    BigDecimal unitPrice,
    BigDecimal lineTotal,
    BigDecimal confidenceUnitPrice,
    List<String> flaggedFields
) {}
