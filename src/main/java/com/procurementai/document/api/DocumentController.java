package com.procurementai.document.api;

import com.procurementai.document.model.*;
import com.procurementai.document.repository.*;
import com.procurementai.document.service.DocumentProcessingService;
import com.procurementai.extraction.model.ExtractionStatus;
import com.procurementai.quote.model.Quote;
import com.procurementai.shared.model.Company;
import com.procurementai.shared.model.User;
import com.procurementai.shared.repository.CompanyRepository;
import com.procurementai.shared.repository.UserRepository;
import com.procurementai.supplier.model.Supplier;
import com.procurementai.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Document upload and processing endpoints.
 *
 * POST /api/v1/documents/upload         — upload a quote PDF
 * GET  /api/v1/documents/{id}           — get document + extraction status
 * POST /api/v1/documents/{id}/approve   — approve extraction result
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    // Demo company/user IDs from V5 seed data — replace with security context once auth is wired
    private static final UUID DEMO_COMPANY_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final UUID DEMO_USER_ID    = UUID.fromString("b0000000-0000-0000-0000-000000000001");
    private static final String API_BASE_PATH = "/api/v1/documents";

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
            .contentType(Objects.requireNonNullElse(file.getContentType(), "application/octet-stream"))
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
        documentProcessingService.processDocumentAsync(
            document.getId(), uploadResult.storageKey(), file.getOriginalFilename());

        log.info("Document {} saved, extraction queued", document.getId());

        String documentUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path(API_BASE_PATH + "/{id}")
            .buildAndExpand(document.getId())
            .toUriString();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new DocumentUploadResponse(
            document.getId(),
            file.getOriginalFilename(),
            DocumentStatus.PROCESSING.name(),
            "Document uploaded successfully. Extraction in progress.",
            documentUrl
        ));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentStatusResponse> getDocumentStatus(
            @PathVariable UUID documentId) {

        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        Optional<ExtractionJob> jobOpt = extractionJobRepository.findByDocumentId(documentId);
        Optional<Quote> quoteOpt = Optional.ofNullable(document.getQuote());

        UUID quoteId = quoteOpt.map(Quote::getId).orElse(null);
        String error = jobOpt.map(ExtractionJob::getErrorMessage).orElse(null);
        String status = jobOpt.map(job -> job.getStatus().name())
            .orElse(document.getStatus().name());

        String supplierName = quoteOpt.map(Quote::getSupplierNameRaw).orElse(null);
        String quoteReference = quoteOpt.map(Quote::getQuoteReference).orElse(null);
        double confidence = quoteOpt
            .flatMap(q -> Optional.ofNullable(q.getExtractionConfidence()))
            .map(java.math.BigDecimal::doubleValue)
            .orElse(0.0);
        boolean requiresReview = quoteOpt.map(Quote::isRequiresReview).orElse(false);

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
