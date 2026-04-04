package com.procurementai.controller;

import com.procurementai.model.*;
import com.procurementai.repository.*;
import com.procurementai.service.DocumentProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Document upload and processing endpoints.
 *
 * POST /api/v1/documents/upload     — upload a quote PDF
 * GET  /api/v1/documents/{id}       — get document + extraction status
 * POST /api/v1/documents/{id}/approve — approve extraction result
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    // Demo company/user IDs from V5 seed data — replace with security context once auth is wired
    private static final UUID DEMO_COMPANY_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final UUID DEMO_USER_ID    = UUID.fromString("b0000000-0000-0000-0000-000000000001");

    private final DocumentRepository       documentRepository;
    private final CompanyRepository        companyRepository;
    private final UserRepository           userRepository;
    private final SupplierRepository       supplierRepository;
    private final ExtractionJobRepository  extractionJobRepository;
    private final DocumentProcessingService documentProcessingService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "supplierId", required = false) UUID supplierId) throws IOException {

        log.info("Upload request: {} ({} bytes)", file.getOriginalFilename(), file.getSize());

        Company company = companyRepository.findById(DEMO_COMPANY_ID)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Demo company not found"));
        User uploader = userRepository.findById(DEMO_USER_ID)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Demo user not found"));
        Supplier supplier = supplierId != null
            ? supplierRepository.findById(supplierId).orElse(null)
            : null;

        // 1. Upload to S3 and get storage key + checksum
        var uploadResult = documentProcessingService.uploadDocument(file, DEMO_COMPANY_ID, DEMO_USER_ID);

        // 2. Persist Document record
        Document document = Document.builder()
            .company(company)
            .uploadedBy(uploader)
            .supplier(supplier)
            .originalFilename(file.getOriginalFilename())
            .storageKey(uploadResult.storageKey())
            .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
            .fileSizeBytes(file.getSize())
            .checksumSha256(uploadResult.checksum())
            .status(DocumentStatus.PROCESSING)
            .build();
        document = documentRepository.save(document);

        // 3. Create ExtractionJob
        ExtractionJob job = ExtractionJob.builder()
            .document(document)
            .status(ExtractionStatus.PENDING)
            .build();
        extractionJobRepository.save(job);

        // 4. Trigger async extraction pipeline (fire-and-forget; persistence handled inside service)
        final UUID documentId = document.getId();
        documentProcessingService.processDocumentAsync(
            documentId, uploadResult.storageKey(), file.getOriginalFilename());

        log.info("Document {} saved, extraction queued", document.getId());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new DocumentUploadResponse(
            document.getId(),
            file.getOriginalFilename(),
            "PROCESSING",
            "Document uploaded successfully. Extraction in progress.",
            "/api/v1/documents/" + document.getId()
        ));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentStatusResponse> getDocumentStatus(
            @PathVariable UUID documentId) {

        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        ExtractionJob job = extractionJobRepository.findByDocumentId(documentId).orElse(null);

        UUID quoteId = document.getQuote() != null ? document.getQuote().getId() : null;
        String error = job != null ? job.getErrorMessage() : null;
        String status = job != null ? job.getStatus().name() : document.getStatus().name();

        String supplierName = null;
        String quoteReference = null;
        double confidence = 0.0;
        boolean requiresReview = false;

        if (document.getQuote() != null) {
            var quote = document.getQuote();
            supplierName = quote.getSupplierNameRaw();
            quoteReference = quote.getQuoteReference();
            confidence = quote.getExtractionConfidence() != null
                ? quote.getExtractionConfidence().doubleValue() : 0.0;
            requiresReview = quote.isRequiresReview();
        }

        return ResponseEntity.ok(new DocumentStatusResponse(
            documentId, quoteId, document.getOriginalFilename(), status,
            supplierName, quoteReference, confidence, requiresReview, error
        ));
    }

    @PostMapping("/{documentId}/approve")
    public ResponseEntity<Void> approveExtraction(@PathVariable UUID documentId) {
        documentRepository.findById(documentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        log.info("Extraction approved for document: {}", documentId);
        return ResponseEntity.ok().build();
    }
}

record DocumentUploadResponse(UUID documentId, String filename, String status, String message, String statusUrl) {}
record DocumentStatusResponse(UUID documentId, UUID quoteId, String filename, String status,
                               String supplierName, String quoteReference, double confidence,
                               boolean requiresReview, String error) {}
