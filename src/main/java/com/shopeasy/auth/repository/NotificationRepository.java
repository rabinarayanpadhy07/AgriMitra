package com.shopeasy.auth.repository;

import com.shopeasy.auth.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientUserIsNullOrderByCreatedAtDesc(Pageable pageable);
    long countByRecipientUserIsNullAndIsReadFalse();
}
