package com.procurementai.comparison.model;

import com.procurementai.quote.model.Quote;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "comparison_quotes")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ComparisonQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comparison_id", nullable = false)
    private Comparison comparison;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    private Integer sortOrder = 0;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
