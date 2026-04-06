package com.procurementai.document.dto;

public record DocumentUploadResult(
    String storageKey,
    String checksum,
    long fileSizeBytes,
    String contentType
) {}
