package com.procurementai.document.model;

import com.procurementai.quote.model.Quote;
import com.procurementai.shared.model.Company;
import com.procurementai.shared.model.User;
import com.procurementai.supplier.model.Supplier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String storageKey;

    @Column(nullable = false)
    private String contentType;

    private Long fileSizeBytes;
    private Integer pageCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.UPLOADED;

    private String processingError;
    private String checksumSha256;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL)
    private Quote quote;
}
