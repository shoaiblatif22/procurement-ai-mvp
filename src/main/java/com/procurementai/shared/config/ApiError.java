package com.procurementai.shared.config;

import java.time.OffsetDateTime;

public record ApiError(
    int status,
    String message,
    Object details,
    OffsetDateTime timestamp
) {}
