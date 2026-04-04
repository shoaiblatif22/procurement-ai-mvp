package com.procurementai.record;

public record DocumentUploadResult (
    // ── Result records ─────────────────────────────────────────────
    String storageKey,
    String checksum,
    long fileSizeBytes,
    String contentType
) {}
    

