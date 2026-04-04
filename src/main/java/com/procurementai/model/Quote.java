package com.procurementai.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quotes")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    private String supplierNameRaw;
    private String quoteReference;
    private LocalDate quoteDate;
    private LocalDate validUntil;

    @Column(length = 3)
    private String currency = "GBP";

    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String paymentTerms;
    private String deliveryTerms;
    private Integer leadTimeDays;
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExtractionStatus extractionStatus = ExtractionStatus.PENDING;

    private BigDecimal extractionConfidence;
    private boolean requiresReview = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private OffsetDateTime reviewedAt;

    @Column(columnDefinition = "jsonb")
    private String rawExtraction;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    private List<QuoteLineItem> lineItems = new ArrayList<>();

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
