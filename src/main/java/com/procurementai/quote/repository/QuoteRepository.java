package com.procurementai.quote.repository;

import com.procurementai.extraction.model.ExtractionStatus;
import com.procurementai.quote.model.Quote;
import com.procurementai.shared.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, UUID> {

    List<Quote> findByCompanyIdAndRequiresReviewTrue(UUID companyId);

    List<Quote> findByCompanyIdAndExtractionStatus(UUID companyId, ExtractionStatus status);

    @Query("SELECT q FROM Quote q WHERE q.company.id = :companyId " +
           "AND q.supplier.id = :supplierId ORDER BY q.createdAt DESC")
    List<Quote> findByCompanyAndSupplier(@Param("companyId") UUID companyId,
                                          @Param("supplierId") UUID supplierId);

    @Modifying
    @Query("UPDATE Quote q SET q.requiresReview = false, q.reviewedBy = :reviewer, " +
           "q.reviewedAt = CURRENT_TIMESTAMP WHERE q.id = :quoteId")
    void approveExtraction(@Param("quoteId") UUID quoteId,
                            @Param("reviewer") User reviewer);
}
