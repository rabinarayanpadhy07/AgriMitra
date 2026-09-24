package com.shopeasy.auth.service;

import com.shopeasy.auth.dto.CouponRequest;
import com.shopeasy.auth.dto.CouponResponse;
import com.shopeasy.auth.dto.admin.PageResponse;
import com.shopeasy.auth.entity.Coupon;
import com.shopeasy.auth.entity.CouponType;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.exception.ApiException;
import com.shopeasy.auth.exception.ResourceNotFoundException;
import com.shopeasy.auth.repository.CouponRepository;
import com.shopeasy.auth.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public PageResponse<CouponResponse> list(Pageable pageable) {
        Page<CouponResponse> page = couponRepository.findAll(pageable).map(CouponResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public CouponResponse getById(Long id) {
        return CouponResponse.from(findById(id));
    }

    @Transactional
    public CouponResponse create(CouponRequest request) {
        if (couponRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new ApiException("Coupon code already exists: " + request.getCode());
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().trim().toUpperCase())
                .type(parseType(request.getType()))
                .value(request.getValue())
                .minOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : BigDecimal.ZERO)
                .maxDiscount(request.getMaxDiscount())
                .startDate(request.getStartDate())
                .expiryDate(request.getExpiryDate())
                .usageLimit(request.getUsageLimit())
                .perUserLimit(request.getPerUserLimit())
                .active(request.getActive() == null || request.getActive())
                .build();

        return CouponResponse.from(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse update(Long id, CouponRequest request) {
        Coupon coupon = findById(id);

        if (!coupon.getCode().equalsIgnoreCase(request.getCode()) && couponRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new ApiException("Coupon code already exists: " + request.getCode());
        }

        coupon.setCode(request.getCode().trim().toUpperCase());
        coupon.setType(parseType(request.getType()));
        coupon.setValue(request.getValue());
        if (request.getMinOrderAmount() != null) coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setMaxDiscount(request.getMaxDiscount());
        coupon.setStartDate(request.getStartDate());
        coupon.setExpiryDate(request.getExpiryDate());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setPerUserLimit(request.getPerUserLimit());
        if (request.getActive() != null) coupon.setActive(request.getActive());

        return CouponResponse.from(couponRepository.save(coupon));
    }

    @Transactional
    public void delete(Long id) {
        couponRepository.delete(findById(id));
    }

    /** Validates eligibility server-side and returns the discount amount (never trusts client-supplied discounts). */
    @Transactional
    public BigDecimal validateAndComputeDiscount(String code, BigDecimal subtotal, User user) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ApiException("Invalid coupon code"));

        LocalDate today = LocalDate.now();
        if (!coupon.getActive()) throw new ApiException("This coupon is no longer active");
        if (today.isBefore(coupon.getStartDate()) || today.isAfter(coupon.getExpiryDate())) {
            throw new ApiException("This coupon is not valid today");
        }
        if (subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new ApiException("Minimum order amount for this coupon is " + coupon.getMinOrderAmount());
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new ApiException("This coupon has reached its usage limit");
        }
        if (coupon.getPerUserLimit() != null) {
            long usedByUser = orderRepository.countByUserAndCouponCode(user, coupon.getCode());
            if (usedByUser >= coupon.getPerUserLimit()) {
                throw new ApiException("You have already used this coupon the maximum number of times");
            }
        }

        BigDecimal discount = coupon.getType() == CouponType.PERCENTAGE
                ? subtotal.multiply(coupon.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : coupon.getValue();

        if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
            discount = coupon.getMaxDiscount();
        }
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }

        return discount;
    }

    @Transactional
    public void recordUsage(String code) {
        couponRepository.findByCodeIgnoreCase(code).ifPresent(c -> {
            c.setUsedCount(c.getUsedCount() + 1);
            couponRepository.save(c);
        });
    }

    private CouponType parseType(String type) {
        try {
            return CouponType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException("Coupon type must be PERCENTAGE or FIXED");
        }
    }

    private Coupon findById(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + id));
    }
}
