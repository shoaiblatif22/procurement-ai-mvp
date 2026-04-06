package com.procurementai.extraction.service;

import com.procurementai.extraction.service.dto.QuoteExtractionResult;
import reactor.core.publisher.Mono;

/**
 * Strategy interface for AI-powered quote data extraction.
 * Implementations can use different LLM providers (Ollama, Claude, Gemini).
 */
public interface QuoteExtractionService {
    Mono<QuoteExtractionResult> extractQuoteData(String ocrText, String originalFilename);
}
