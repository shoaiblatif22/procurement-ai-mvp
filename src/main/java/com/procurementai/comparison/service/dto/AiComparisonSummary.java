package com.procurementai.comparison.service.dto;

public record AiComparisonSummary(
    String rawResponse,
    String parsedRecommendation
) {}
