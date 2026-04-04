package com.procurementai.service;

import java.math.BigDecimal;

public record ComparisonSummary(
    int quoteCount,
    QuoteSnapshot cheapestQuote,
    QuoteSnapshot mostExpensiveQuote,
    BigDecimal potentialSaving,
    int missingItemCount
) {}
