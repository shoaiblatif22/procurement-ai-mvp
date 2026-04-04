package com.procurementai.repository;

import com.procurementai.model.QuoteLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuoteLineItemRepository extends JpaRepository<QuoteLineItem, UUID> {

    List<QuoteLineItem> findByQuoteIdOrderByLineNumberAsc(UUID quoteId);

    @Query("SELECT li FROM QuoteLineItem li WHERE li.quote.id IN :quoteIds " +
           "ORDER BY li.quote.id, li.lineNumber")
    List<QuoteLineItem> findByQuoteIds(@Param("quoteIds") List<UUID> quoteIds);
}
