package com.procurementai.service;

import java.util.List;

public record MatchedLineGroup(
    String normalisedDescription,
    List<MatchedItem> itemsBySupplier,
    MatchedItem cheapest,
    MatchedItem mostExpensive,
    boolean missingFromSomeQuotes
) {}
