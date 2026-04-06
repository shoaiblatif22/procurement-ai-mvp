package com.procurementai.comparison.service.dto;

import java.util.UUID;

public record MatchedItem(
    UUID supplierId,
    String supplierName,
    LineItemSnapshot lineItem
) {}
