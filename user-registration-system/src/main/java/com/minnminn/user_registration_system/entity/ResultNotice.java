package com.minnminn.user_registration_system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Snapshot of a generated Result Notice (Phase 3 report).
 */
@Entity
@Table(name = "result_notices")
@Data
public class ResultNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "financial_institution_id", nullable = false)
    private FinancialInstitution financialInstitution;

    @Column(name = "notice_date", nullable = false)
    private LocalDate noticeDate;

    @Column(name = "vpn_status", length = 64)
    private String vpnStatus;

    @Column(name = "fides_login_status", length = 64)
    private String fidesLoginStatus;

    @Column(name = "user_category", length = 128)
    private String userCategory;

    @Column(name = "vpn_user_id")
    private String vpnUserId;

    @Column(name = "vpn_password")
    private String vpnPassword;

    @Column(name = "vpn_psk")
    private String vpnPsk;

    @Column(name = "login_user_id")
    private String loginUserId;

    @Column(name = "login_password")
    private String loginPassword;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
