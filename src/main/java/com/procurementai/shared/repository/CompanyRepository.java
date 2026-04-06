package com.procurementai.shared.repository;

import com.procurementai.shared.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Optional<Company> findByDomain(String domain);
    boolean existsByDomain(String domain);
}
