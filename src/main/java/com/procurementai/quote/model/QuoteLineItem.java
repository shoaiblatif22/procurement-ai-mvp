package com.procurementai.quote.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "quote_line_items")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QuoteLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @Column(nullable = false)
    private Integer lineNumber;

    @Column(nullable = false)
    private String description;

    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal discountPct;
    private BigDecimal lineTotal;
    private String sku;

    private BigDecimal confidenceDescription;
    private BigDecimal confidenceQuantity;
    private BigDecimal confidenceUnitPrice;
    private BigDecimal confidenceLineTotal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String flaggedFields;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
