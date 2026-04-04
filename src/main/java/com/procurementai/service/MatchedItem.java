package com.procurementai.service;

import java.util.UUID;

public record MatchedItem(
    UUID supplierId,
    String supplierName,
    LineItemSnapshot lineItem
) {}
