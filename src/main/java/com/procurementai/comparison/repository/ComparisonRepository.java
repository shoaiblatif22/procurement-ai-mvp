package com.procurementai.comparison.repository;

import com.procurementai.comparison.model.Comparison;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComparisonRepository extends JpaRepository<Comparison, UUID> {

    Page<Comparison> findByCompanyIdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);

    List<Comparison> findByCompanyIdAndStatus(UUID companyId, String status);

    @Query("SELECT c FROM Comparison c JOIN c.comparisonQuotes cq " +
           "WHERE cq.quote.id = :quoteId")
    List<Comparison> findByQuoteId(@Param("quoteId") UUID quoteId);
}
