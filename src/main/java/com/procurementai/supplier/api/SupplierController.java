package com.procurementai.supplier.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Supplier management endpoints.
 */
@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Slf4j
public class SupplierController {

    @GetMapping
    public ResponseEntity<List<SupplierSummary>> listSuppliers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {

        // TODO: fetch from DB with optional search
        List<SupplierSummary> suppliers = List.of(
            new SupplierSummary(UUID.fromString("c0000000-0000-0000-0000-000000000001"),
                "BuildRight Materials Ltd", "Manchester", true, 12),
            new SupplierSummary(UUID.fromString("c0000000-0000-0000-0000-000000000002"),
                "Northern Steel Supplies", "Leeds", false, 4),
            new SupplierSummary(UUID.fromString("c0000000-0000-0000-0000-000000000003"),
                "UK Construction Wholesale", "Birmingham", false, 7)
        );

        return ResponseEntity.ok(suppliers);
    }

    @PostMapping
    public ResponseEntity<SupplierSummary> createSupplier(
            @RequestBody CreateSupplierRequest request) {

        // TODO: persist to DB
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new SupplierSummary(UUID.randomUUID(), request.name(), request.city(), false, 0));
    }
}

record SupplierSummary(UUID id, String name, String city, boolean isPreferred, int quoteCount) {}
record CreateSupplierRequest(String name, String city) {}
