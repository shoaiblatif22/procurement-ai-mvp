package com.procurementai.repository;

import com.procurementai.model.ExtractionJob;
import com.procurementai.model.ExtractionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExtractionJobRepository extends JpaRepository<ExtractionJob, UUID> {

    List<ExtractionJob> findByStatusAndAttemptsLessThan(ExtractionStatus status, int maxAttempts);

    Optional<ExtractionJob> findByDocumentId(UUID documentId);
}
