package com.shopeasy.auth.service;

import com.shopeasy.auth.dto.ReturnRequestCreateRequest;
import com.shopeasy.auth.dto.ReturnRequestResponse;
import com.shopeasy.auth.dto.admin.PageResponse;
import com.shopeasy.auth.entity.*;
import com.shopeasy.auth.exception.ApiException;
import com.shopeasy.auth.exception.ResourceNotFoundException;
import com.shopeasy.auth.repository.OrderItemRepository;
import com.shopeasy.auth.repository.OrderRepository;
import com.shopeasy.auth.repository.PaymentRepository;
import com.shopeasy.auth.repository.ReturnRequestRepository;
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
public class ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

    @Transactional
    public ReturnRequestResponse create(User user, ReturnRequestCreateRequest request) {
        Order order = orderRepository.findByIdAndUser(request.getOrderId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ApiException("Only delivered orders can be returned");
        }

        OrderItem orderItem = null;
        if (request.getOrderItemId() != null) {
            orderItem = orderItemRepository.findById(request.getOrderItemId())
                    .filter(i -> i.getOrder().getId().equals(order.getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Order item not found on this order"));
        }

        ReturnRequest returnRequest = ReturnRequest.builder()
                .order(order)
                .orderItem(orderItem)
                .user(user)
                .reason(request.getReason())
                .status(ReturnStatus.REQUESTED)
                .build();
        returnRequest = returnRequestRepository.save(returnRequest);

        order.setStatus(OrderStatus.RETURN_REQUESTED);
        orderRepository.save(order);

        notificationService.notifyAdmins(NotificationType.RETURN_REQUEST,
                "New return request", user.getFullName() + " requested a return for order #" + order.getId());

        return ReturnRequestResponse.from(returnRequest);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReturnRequestResponse> myReturns(User user, Pageable pageable) {
        Page<ReturnRequestResponse> page = returnRequestRepository
                .findByUserOrderByRequestedAtDesc(user, pageable)
                .map(ReturnRequestResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReturnRequestResponse> adminList(String status, Pageable pageable) {
        Specification<ReturnRequest> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), ReturnStatus.valueOf(status.trim().toUpperCase())));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<ReturnRequestResponse> page = returnRequestRepository.findAll(spec, pageable).map(ReturnRequestResponse::from);
        return PageResponse.from(page);
    }

    @Transactional
    public ReturnRequestResponse updateStatus(Long id, String statusValue) {
        ReturnRequest returnRequest = findById(id);
        ReturnStatus newStatus;
        try {
            newStatus = ReturnStatus.valueOf(statusValue.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid return status: " + statusValue);
        }

        returnRequest.setStatus(newStatus);
        returnRequestRepository.save(returnRequest);

        Order order = returnRequest.getOrder();
        if (newStatus == ReturnStatus.REFUNDED) {
            order.setStatus(OrderStatus.REFUNDED);
            orderRepository.save(order);
            paymentRepository.findByOrder(order).ifPresent(p -> {
                p.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(p);
            });
        } else if (newStatus == ReturnStatus.RECEIVED) {
            order.setStatus(OrderStatus.RETURNED);
            orderRepository.save(order);
        }

        return ReturnRequestResponse.from(returnRequest);
    }

    private ReturnRequest findById(Long id) {
        return returnRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found with id: " + id));
    }
}
