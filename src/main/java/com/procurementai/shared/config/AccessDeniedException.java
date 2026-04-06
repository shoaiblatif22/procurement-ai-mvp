package com.procurementai.shared.config;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) { super(message); }
}
