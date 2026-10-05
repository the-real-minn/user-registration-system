package com.minnminn.user_registration_system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(
        name = "credentials",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_credential_fi_system",
                columnNames = {"financial_institution_id", "system_type_id"}
        )
)
@Data
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "financial_institution_id", nullable = false)
    private FinancialInstitution financialInstitution;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "system_type_id", nullable = false)
    private SystemType systemType;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "password")
    private String password;

    /** VPN only. */
    @Column(name = "pre_shared_key")
    private String preSharedKey;

    @Column(name = "update_date")
    private LocalDate updateDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
