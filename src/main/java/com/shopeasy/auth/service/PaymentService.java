package com.shopeasy.auth.service;

import com.shopeasy.auth.dto.PaymentResponse;
import com.shopeasy.auth.dto.admin.PageResponse;
import com.shopeasy.auth.entity.NotificationType;
import com.shopeasy.auth.entity.Payment;
import com.shopeasy.auth.entity.PaymentStatus;
import com.shopeasy.auth.exception.ApiException;
import com.shopeasy.auth.exception.ResourceNotFoundException;
import com.shopeasy.auth.repository.PaymentRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> list(String status, Pageable pageable) {
        Specification<Payment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), PaymentStatus.valueOf(status.trim().toUpperCase())));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<PaymentResponse> page = paymentRepository.findAll(spec, pageable).map(PaymentResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        return PaymentResponse.from(findById(id));
    }

    @Transactional
    public PaymentResponse updateStatus(Long id, String statusValue) {
        Payment payment = findById(id);
        PaymentStatus newStatus;
        try {
            newStatus = PaymentStatus.valueOf(statusValue.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid payment status: " + statusValue);
        }

        payment.setStatus(newStatus);
        paymentRepository.save(payment);

        if (newStatus == PaymentStatus.FAILED) {
            notificationService.notifyAdmins(NotificationType.PAYMENT_FAILURE,
                    "Payment failed", "Payment for order #" + payment.getOrder().getId() + " failed");
        }

        return PaymentResponse.from(payment);
    }

    private Payment findById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }
}
