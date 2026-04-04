package com.procurementai.service;

public record AiComparisonSummary(
    String rawResponse,
    String parsedRecommendation
) {}
