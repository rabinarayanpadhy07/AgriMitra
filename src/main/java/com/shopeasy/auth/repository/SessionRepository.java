package com.shopeasy.auth.repository;

import com.shopeasy.auth.entity.Session;
import com.shopeasy.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByJwtTokenAndIsActiveTrue(String jwtToken);

    Optional<Session> findFirstByJwtTokenAndIsActiveTrueOrderByIdDesc(String jwtToken);

    List<Session> findByUserAndIsActiveTrueOrderByLoginTimeDesc(User user);

    @Modifying
    @Query("UPDATE Session s SET s.isActive = false WHERE s.user = :user AND s.isActive = true")
    void invalidateAllUserSessions(@Param("user") User user);

    @Modifying
    @Query("UPDATE Session s SET s.isActive = false WHERE s.jwtToken = :jwtToken")
    void invalidateSessionByToken(@Param("jwtToken") String jwtToken);
}
