package com.procurementai.repository;

import com.procurementai.model.ComparisonQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComparisonQuoteRepository extends JpaRepository<ComparisonQuote, UUID> {

    List<ComparisonQuote> findByComparisonIdOrderBySortOrderAsc(UUID comparisonId);

    boolean existsByComparisonIdAndQuoteId(UUID comparisonId, UUID quoteId);
}
