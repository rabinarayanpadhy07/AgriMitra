package com.shopeasy.auth.service;

import com.shopeasy.auth.dto.ReviewRequest;
import com.shopeasy.auth.dto.ReviewResponse;
import com.shopeasy.auth.dto.admin.PageResponse;
import com.shopeasy.auth.entity.*;
import com.shopeasy.auth.exception.ApiException;
import com.shopeasy.auth.exception.ResourceNotFoundException;
import com.shopeasy.auth.repository.OrderRepository;
import com.shopeasy.auth.repository.ProductRepository;
import com.shopeasy.auth.repository.ReviewRepository;
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
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    @Transactional
    public ReviewResponse create(User user, ReviewRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (reviewRepository.existsByUserAndProduct(user, product)) {
            throw new ApiException("You have already reviewed this product");
        }

        Order order = null;
        if (request.getOrderId() != null) {
            order = orderRepository.findByIdAndUser(request.getOrderId(), user).orElse(null);
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .order(order)
                .rating(request.getRating())
                .comment(request.getComment())
                .status(ReviewStatus.PENDING)
                .build();
        review = reviewRepository.save(review);

        notificationService.notifyAdmins(NotificationType.NEW_REVIEW,
                "New review submitted", user.getFullName() + " reviewed " + product.getName());

        return ReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> publicListForProduct(Long productId, Pageable pageable) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Page<ReviewResponse> page = reviewRepository
                .findByProductAndStatusOrderByCreatedAtDesc(product, ReviewStatus.APPROVED, pageable)
                .map(ReviewResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> adminList(String status, Pageable pageable) {
        Specification<Review> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), ReviewStatus.valueOf(status.trim().toUpperCase())));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<ReviewResponse> page = reviewRepository.findAll(spec, pageable).map(ReviewResponse::from);
        return PageResponse.from(page);
    }

    @Transactional
    public ReviewResponse approve(Long id) {
        Review review = findById(id);
        review.setStatus(ReviewStatus.APPROVED);
        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse reject(Long id) {
        Review review = findById(id);
        review.setStatus(ReviewStatus.REJECTED);
        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional
    public void delete(Long id) {
        reviewRepository.delete(findById(id));
    }

    private Review findById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
    }
}
