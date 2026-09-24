package com.shopeasy.auth.repository;

import com.shopeasy.auth.entity.Order;
import com.shopeasy.auth.entity.Payment;
import com.shopeasy.auth.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    Optional<Payment> findByOrder(Order order);
    long countByStatus(PaymentStatus status);
}
