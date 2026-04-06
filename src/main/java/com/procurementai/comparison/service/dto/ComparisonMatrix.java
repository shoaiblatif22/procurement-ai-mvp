package com.procurementai.comparison.service.dto;

import java.util.List;

public record ComparisonMatrix(
    List<QuoteSnapshot> quotes,
    List<MatchedLineGroup> matchedGroups,
    ComparisonSummary summary
) {}
