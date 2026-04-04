package com.procurementai.service;

import java.util.List;

public record ComparisonMatrix(
    List<QuoteSnapshot> quotes,
    List<MatchedLineGroup> matchedGroups,
    ComparisonSummary summary
) {}
