package com.shopeasy.auth.repository;

import com.shopeasy.auth.entity.ReturnRequest;
import com.shopeasy.auth.entity.ReturnStatus;
import com.shopeasy.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long>, JpaSpecificationExecutor<ReturnRequest> {
    Page<ReturnRequest> findByUserOrderByRequestedAtDesc(User user, Pageable pageable);
    long countByStatus(ReturnStatus status);
}
