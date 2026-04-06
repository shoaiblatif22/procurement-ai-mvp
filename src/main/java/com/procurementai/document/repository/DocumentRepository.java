package com.procurementai.document.repository;

import com.procurementai.document.model.Document;
import com.procurementai.document.model.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Page<Document> findByCompanyIdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);

    List<Document> findByCompanyIdAndStatus(UUID companyId, DocumentStatus status);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.company.id = :companyId " +
           "AND d.createdAt >= :since")
    long countDocumentsUsedSince(@Param("companyId") UUID companyId,
                                  @Param("since") OffsetDateTime since);

    Optional<Document> findByChecksumSha256AndCompanyId(String checksum, UUID companyId);
}
