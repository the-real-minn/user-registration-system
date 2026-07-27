package com.minnminn.user_registration_system.repository;

import com.minnminn.user_registration_system.entity.SystemType;
import com.minnminn.user_registration_system.entity.SystemTypeCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SystemTypeRepository extends JpaRepository<SystemType, Long> {

    Optional<SystemType> findByCode(SystemTypeCode code);

    List<SystemType> findAllByOrderBySortOrderAsc();
}
