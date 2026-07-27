package com.minnminn.user_registration_system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "financial_institutions")
@Data
public class FinancialInstitution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fi_code", nullable = false, unique = true, length = 32)
    private String fiCode;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "short_title", length = 64)
    private String shortTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "row_highlight", length = 32)
    private RowHighlight rowHighlight = RowHighlight.NORMAL;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
