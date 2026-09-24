package com.shopeasy.auth.service;

import com.shopeasy.auth.dto.NotificationResponse;
import com.shopeasy.auth.dto.admin.PageResponse;
import com.shopeasy.auth.entity.Notification;
import com.shopeasy.auth.entity.NotificationType;
import com.shopeasy.auth.exception.ResourceNotFoundException;
import com.shopeasy.auth.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reusable notification infrastructure. Broadcasts (recipientUser = null) feed the
 * admin notification center. Triggered by other services on key platform events.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void notifyAdmins(NotificationType type, String title, String message) {
        Notification notification = Notification.builder()
                .recipientUser(null)
                .type(type)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listAdminNotifications(Pageable pageable) {
        Page<NotificationResponse> page = notificationRepository
                .findByRecipientUserIsNullOrderByCreatedAtDesc(pageable)
                .map(NotificationResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByRecipientUserIsNullAndIsReadFalse();
    }

    @Transactional
    public NotificationResponse markRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        notification.setIsRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }
}
