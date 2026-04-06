package com.procurementai.supplier.repository;

import com.procurementai.supplier.model.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    Page<Supplier> findByCompanyIdAndIsActiveTrue(UUID companyId, Pageable pageable);

    List<Supplier> findByCompanyIdAndIsPreferredTrue(UUID companyId);

    @Query(value = """
        SELECT * FROM suppliers
        WHERE company_id = :companyId
          AND is_active = true
          AND similarity(name, :name) > 0.3
        ORDER BY similarity(name, :name) DESC
        LIMIT 5
        """, nativeQuery = true)
    List<Supplier> findSimilarByName(@Param("companyId") UUID companyId,
                                     @Param("name") String name);
}
