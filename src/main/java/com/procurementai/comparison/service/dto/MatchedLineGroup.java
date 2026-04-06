package com.procurementai.comparison.service.dto;

import java.util.List;

public record MatchedLineGroup(
    String normalisedDescription,
    List<MatchedItem> itemsBySupplier,
    MatchedItem cheapest,
    MatchedItem mostExpensive,
    boolean missingFromSomeQuotes
) {}
