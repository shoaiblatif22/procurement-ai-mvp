package com.procurementai.document.model;

import com.procurementai.extraction.model.ExtractionStatus;
import com.procurementai.quote.model.Quote;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "extraction_jobs")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ExtractionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id")
    private Quote quote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExtractionStatus status = ExtractionStatus.PENDING;

    @Builder.Default private int attempts = 0;
    @Builder.Default private int maxAttempts = 3;
    private String errorMessage;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
