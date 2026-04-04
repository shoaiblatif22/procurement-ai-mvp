package com.procurementai.service;

import com.procurementai.integration.ClaudeExtractionService;
import com.procurementai.integration.QuoteExtractionResult;
import com.procurementai.record.DocumentUploadResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.Block;
import software.amazon.awssdk.services.textract.model.BlockType;
import software.amazon.awssdk.services.textract.model.DetectDocumentTextRequest;
import software.amazon.awssdk.services.textract.model.DetectDocumentTextResponse;
import software.amazon.awssdk.services.textract.model.Document;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Orchestrates the full document processing pipeline:
 * 1. Upload file to S3
 * 2. Run AWS Textract OCR
 * 3. Send OCR text to Claude for structured extraction
 * 4. Persist results via ExtractionPersistenceService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final S3Client s3Client;
    private final TextractClient textractClient;
    private final ClaudeExtractionService claudeExtractionService;
    private final ExtractionPersistenceService extractionPersistenceService;

    private final String s3Bucket;

    @Value("${app.ocr.use-pdfbox:true}")
    private boolean usePdfBox;

    // ── Upload ─────────────────────────────────────────────────

    public DocumentUploadResult uploadDocument(MultipartFile file, UUID companyId, UUID userId)
            throws IOException {

        validateFile(file);

        String storageKey = buildStorageKey(companyId, file.getOriginalFilename());
        String checksum = computeChecksum(file.getBytes());

        log.info("Uploading document: {} for company: {}", file.getOriginalFilename(), companyId);

        ensureBucketExists(s3Bucket);

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(storageKey)
                .contentType(file.getContentType())
                .metadata(java.util.Map.of(
                    "company-id", companyId.toString(),
                    "uploaded-by", userId.toString(),
                    "original-filename", file.getOriginalFilename(),
                    "checksum-sha256", checksum
                ))
                .serverSideEncryption("AES256")
                .build(),
            RequestBody.fromBytes(file.getBytes())
        );

        log.info("Document uploaded to S3: {}", storageKey);
        return new DocumentUploadResult(storageKey, checksum, file.getSize(), file.getContentType());
    }

    // ── Text extraction ──────────────────────────────────────

    public String extractTextFromDocument(String storageKey) {
        if (usePdfBox) {
            return extractTextWithPdfBox(storageKey);
        }
        return extractTextWithTextract(storageKey);
    }

    private String extractTextWithPdfBox(String storageKey) {
        log.info("Running PDFBox text extraction on: {}", storageKey);

        try {
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(
                GetObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(storageKey)
                    .build()
            );

            byte[] pdfBytes = s3Object.readAllBytes();

            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String extractedText = stripper.getText(document);
                log.info("PDFBox extracted {} characters from {}", extractedText.length(), storageKey);
                return extractedText;
            }

        } catch (Exception e) {
            log.error("PDFBox extraction failed for {}: {}", storageKey, e.getMessage());
            throw new OcrException("PDF text extraction failed: " + e.getMessage(), e);
        }
    }

    private String extractTextWithTextract(String storageKey) {
        log.info("Running Textract OCR on: {}", storageKey);

        try {
            DetectDocumentTextRequest request = DetectDocumentTextRequest.builder()
                .document(Document.builder()
                    .s3Object(software.amazon.awssdk.services.textract.model.S3Object.builder()
                        .bucket(s3Bucket)
                        .name(storageKey)
                        .build())
                    .build())
                .build();

            DetectDocumentTextResponse response = textractClient.detectDocumentText(request);

            String extractedText = response.blocks().stream()
                .filter(block -> block.blockType() == BlockType.LINE)
                .map(Block::text)
                .collect(Collectors.joining("\n"));

            log.info("Textract extracted {} characters from {}", extractedText.length(), storageKey);
            return extractedText;

        } catch (Exception e) {
            log.error("Textract failed for {}: {}", storageKey, e.getMessage());
            throw new OcrException("OCR extraction failed: " + e.getMessage(), e);
        }
    }

    // ── Full async pipeline ────────────────────────────────────

    /**
     * Run OCR + Claude extraction async. Caller must supply documentId so results
     * can be persisted back to the DB once Claude responds.
     */
    @Async
    public CompletableFuture<QuoteExtractionResult> processDocumentAsync(
            UUID documentId,
            String storageKey,
            String originalFilename) {

        log.info("Starting async processing pipeline for document {}", documentId);

        return CompletableFuture
            .supplyAsync(() -> extractTextFromDocument(storageKey))
            .thenCompose(ocrText ->
                claudeExtractionService
                    .extractQuoteData(ocrText, originalFilename)
                    .toFuture()
            )
            .whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Pipeline failed for document {}: {}", documentId, error.getMessage());
                    extractionPersistenceService.markFailed(documentId, error.getMessage());
                } else {
                    log.info("Pipeline complete for document {}. Confidence: {}",
                        documentId, result.overallConfidence());
                    extractionPersistenceService.persistExtractionResult(documentId, result);
                }
            });
    }

    // ── Helpers ────────────────────────────────────────────────

    private void ensureBucketExists(String bucket) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            log.warn("S3 bucket '{}' not found — creating it now", bucket);
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("S3 bucket '{}' created", bucket);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");

        List<String> allowedTypes = List.of(
            "application/pdf", "image/jpeg", "image/png", "image/tiff",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
        if (!allowedTypes.contains(file.getContentType())) {
            throw new IllegalArgumentException(
                "Unsupported file type: " + file.getContentType());
        }
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new IllegalArgumentException("File exceeds maximum size of 20MB");
        }
    }

    private String buildStorageKey(UUID companyId, String originalFilename) {
        String sanitised = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "documents/%s/%s/%s".formatted(companyId, UUID.randomUUID(), sanitised);
    }

    private String computeChecksum(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception e) {
            return "unknown";
        }
    }
}



class OcrException extends RuntimeException {
    OcrException(String message, Throwable cause) { super(message, cause); }
}
