package com.procurementai.shared.config;

public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException() { super("Monthly document quota exceeded"); }
}
