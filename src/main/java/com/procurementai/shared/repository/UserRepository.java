package com.procurementai.shared.repository;

import com.procurementai.shared.model.User;
import com.procurementai.shared.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByCompanyIdAndIsActiveTrue(UUID companyId);

    @Query("SELECT u FROM User u WHERE u.company.id = :companyId AND u.role = :role")
    List<User> findByCompanyIdAndRole(@Param("companyId") UUID companyId,
                                      @Param("role") UserRole role);
}
