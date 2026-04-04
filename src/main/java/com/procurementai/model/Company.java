package com.procurementai.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String domain;
    private String industry;
    private String size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionTier subscription = SubscriptionTier.FREE;

    private Integer monthlyDocLimit = 20;
    private Integer docsUsedThisMonth = 0;
    private String stripeCustomerId;
    private boolean isActive = true;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Supplier> suppliers = new ArrayList<>();
}
