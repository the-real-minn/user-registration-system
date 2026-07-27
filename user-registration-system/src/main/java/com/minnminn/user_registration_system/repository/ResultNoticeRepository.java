package com.minnminn.user_registration_system.repository;

import com.minnminn.user_registration_system.entity.ResultNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResultNoticeRepository extends JpaRepository<ResultNotice, Long> {

    List<ResultNotice> findByFinancialInstitutionIdOrderByCreatedAtDesc(Long financialInstitutionId);

    void deleteByFinancialInstitutionId(Long financialInstitutionId);
}
