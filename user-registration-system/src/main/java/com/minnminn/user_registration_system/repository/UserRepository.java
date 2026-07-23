package com.minnminn.user_registration_system.repository;

import com.minnminn.user_registration_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);

}