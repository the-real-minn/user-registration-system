package com.minnminn.user_registration_system.repository;

import com.minnminn.user_registration_system.entity.Credential;
import com.minnminn.user_registration_system.entity.FinancialInstitution;
import com.minnminn.user_registration_system.entity.SystemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {

    List<Credential> findByFinancialInstitutionOrderBySystemTypeSortOrderAsc(FinancialInstitution fi);

    Optional<Credential> findByFinancialInstitutionAndSystemType(
            FinancialInstitution fi, SystemType systemType);

    List<Credential> findByFinancialInstitutionId(Long financialInstitutionId);
}
