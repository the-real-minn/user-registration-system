package com.minnminn.user_registration_system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "system_types")
@Data
public class SystemType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 32)
    private SystemTypeCode code;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    /** Optional User ID suffix used with LEFT(fi_code, 4). */
    @Column(name = "id_suffix", length = 32)
    private String idSuffix;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
