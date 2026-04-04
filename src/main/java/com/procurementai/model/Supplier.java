package com.procurementai.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "suppliers")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String name;

    private String tradingName;
    private String email;
    private String phone;
    private String website;
    private String addressLine1;
    private String city;
    private String postcode;
    private String companiesHouseNo;
    private String vatNumber;
    private LocalDate insuranceExpiry;
    private boolean isPreferred = false;
    private boolean isActive = true;
    private String notes;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
