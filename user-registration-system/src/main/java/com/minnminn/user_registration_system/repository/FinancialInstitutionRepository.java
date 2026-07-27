package com.minnminn.user_registration_system.repository;

import com.minnminn.user_registration_system.entity.FinancialInstitution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinancialInstitutionRepository extends JpaRepository<FinancialInstitution, Long> {

    Optional<FinancialInstitution> findByFiCode(String fiCode);

    List<FinancialInstitution> findAllByOrderBySortOrderAscBankNameAsc();

    List<FinancialInstitution> findByBankNameContainingIgnoreCaseOrFiCodeContainingIgnoreCaseOrderBySortOrderAsc(
            String bankName, String fiCode);
}
