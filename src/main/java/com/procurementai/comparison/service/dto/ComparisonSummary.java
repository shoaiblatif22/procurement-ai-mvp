package com.procurementai.comparison.service.dto;

import java.math.BigDecimal;

public record ComparisonSummary(
    int quoteCount,
    QuoteSnapshot cheapestQuote,
    QuoteSnapshot mostExpensiveQuote,
    BigDecimal potentialSaving,
    int missingItemCount
) {}
