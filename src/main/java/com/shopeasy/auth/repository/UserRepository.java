package com.shopeasy.auth.repository;

import com.shopeasy.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByMobileNumber(String mobileNumber);

    boolean existsByEmail(String email);

    boolean existsByMobileNumber(String mobileNumber);

    @Query("SELECT u FROM User u WHERE u.email = :identifier OR u.mobileNumber = :identifier")
    Optional<User> findByEmailOrMobileNumber(@Param("identifier") String identifier);
}
