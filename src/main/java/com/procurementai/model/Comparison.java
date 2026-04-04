package com.procurementai.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "comparisons")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Comparison {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private String title;

    private String description;
    private String category;

    @Column(nullable = false)
    private String status = "DRAFT";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decision_quote_id")
    private Quote decisionQuote;

    private String decisionReason;
    private OffsetDateTime decidedAt;
    private String aiSummary;
    private String aiRecommendation;
    private BigDecimal totalPotentialSaving;

    @OneToMany(mappedBy = "comparison", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ComparisonQuote> comparisonQuotes = new ArrayList<>();

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
