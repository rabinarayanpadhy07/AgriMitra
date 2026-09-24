package com.shopeasy.auth.repository;

import com.shopeasy.auth.entity.AccountStatus;
import com.shopeasy.auth.entity.SellerStatus;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByMobileNumber(String mobileNumber);

    boolean existsByEmail(String email);

    boolean existsByMobileNumber(String mobileNumber);

    @Query("SELECT u FROM User u WHERE u.email = :identifier OR u.mobileNumber = :identifier")
    Optional<User> findByEmailOrMobileNumber(@Param("identifier") String identifier);

    long countByRole(UserRole role);

    long countByAccountStatus(AccountStatus accountStatus);

    long countBySellerStatus(SellerStatus sellerStatus);

    long countByRoleAndAccountStatus(UserRole role, AccountStatus accountStatus);

    long countByCreatedAtAfter(LocalDateTime after);

    List<User> findTop5ByOrderByCreatedAtDesc();

    List<User> findTop5BySellerStatusOrderBySellerAppliedAtDesc(SellerStatus sellerStatus);
}
