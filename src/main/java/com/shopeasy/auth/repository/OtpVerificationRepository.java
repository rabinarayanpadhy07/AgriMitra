package com.shopeasy.auth.repository;

import com.shopeasy.auth.entity.OtpVerification;
import com.shopeasy.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(User user);

    @Modifying
    @Query("UPDATE OtpVerification o SET o.isUsed = true WHERE o.user = :user")
    void markAllOtpsAsUsedForUser(@Param("user") User user);
}
