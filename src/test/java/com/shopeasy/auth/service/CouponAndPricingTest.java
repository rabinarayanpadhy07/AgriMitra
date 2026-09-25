package com.shopeasy.auth.service;

import com.shopeasy.auth.entity.Coupon;
import com.shopeasy.auth.entity.CouponType;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.exception.ApiException;
import com.shopeasy.auth.repository.CouponRepository;
import com.shopeasy.auth.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponAndPricingTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private CouponService couponService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("farmer@agrimitra.com")
                .fullName("Ramesh Farmer")
                .build();
    }

    @Test
    @DisplayName("Should correctly calculate percentage discount")
    void testPercentageDiscountCalculation() {
        Coupon coupon = Coupon.builder()
                .code("AGRI20")
                .type(CouponType.PERCENTAGE)
                .value(new BigDecimal("20.00"))
                .minOrderAmount(new BigDecimal("500.00"))
                .active(true)
                .startDate(LocalDate.now().minusDays(5))
                .expiryDate(LocalDate.now().plusDays(5))
                .usedCount(10)
                .usageLimit(100)
                .build();

        when(couponRepository.findByCodeIgnoreCase("AGRI20")).thenReturn(Optional.of(coupon));

        BigDecimal subtotal = new BigDecimal("1000.00");
        BigDecimal discount = couponService.validateAndComputeDiscount("AGRI20", subtotal, testUser);

        assertEquals(new BigDecimal("200.00"), discount);
    }

    @Test
    @DisplayName("Should cap percentage discount at maxDiscount limit")
    void testPercentageDiscountWithMaxCap() {
        Coupon coupon = Coupon.builder()
                .code("HARVEST50")
                .type(CouponType.PERCENTAGE)
                .value(new BigDecimal("50.00"))
                .maxDiscount(new BigDecimal("250.00"))
                .minOrderAmount(new BigDecimal("200.00"))
                .active(true)
                .startDate(LocalDate.now().minusDays(1))
                .expiryDate(LocalDate.now().plusDays(10))
                .usedCount(0)
                .build();

        when(couponRepository.findByCodeIgnoreCase("HARVEST50")).thenReturn(Optional.of(coupon));

        BigDecimal subtotal = new BigDecimal("1000.00"); // 50% would be 500, but capped at 250
        BigDecimal discount = couponService.validateAndComputeDiscount("HARVEST50", subtotal, testUser);

        assertEquals(new BigDecimal("250.00"), discount);
    }

    @Test
    @DisplayName("Should apply fixed amount discount correctly")
    void testFixedDiscountCalculation() {
        Coupon coupon = Coupon.builder()
                .code("FLAT150")
                .type(CouponType.FIXED)
                .value(new BigDecimal("150.00"))
                .minOrderAmount(new BigDecimal("500.00"))
                .active(true)
                .startDate(LocalDate.now().minusDays(1))
                .expiryDate(LocalDate.now().plusDays(1))
                .usedCount(5)
                .build();

        when(couponRepository.findByCodeIgnoreCase("FLAT150")).thenReturn(Optional.of(coupon));

        BigDecimal subtotal = new BigDecimal("600.00");
        BigDecimal discount = couponService.validateAndComputeDiscount("FLAT150", subtotal, testUser);

        assertEquals(new BigDecimal("150.00"), discount);
    }

    @Test
    @DisplayName("Fixed discount cannot exceed subtotal amount")
    void testFixedDiscountCappedAtSubtotal() {
        Coupon coupon = Coupon.builder()
                .code("MEGA500")
                .type(CouponType.FIXED)
                .value(new BigDecimal("500.00"))
                .minOrderAmount(BigDecimal.ZERO)
                .active(true)
                .startDate(LocalDate.now().minusDays(1))
                .expiryDate(LocalDate.now().plusDays(1))
                .usedCount(0)
                .build();

        when(couponRepository.findByCodeIgnoreCase("MEGA500")).thenReturn(Optional.of(coupon));

        BigDecimal subtotal = new BigDecimal("350.00");
        BigDecimal discount = couponService.validateAndComputeDiscount("MEGA500", subtotal, testUser);

        assertEquals(new BigDecimal("350.00"), discount);
    }

    @Test
    @DisplayName("Should reject coupon when subtotal does not meet minOrderAmount")
    void testMinOrderAmountRejection() {
        Coupon coupon = Coupon.builder()
                .code("MIN1000")
                .type(CouponType.FIXED)
                .value(new BigDecimal("100.00"))
                .minOrderAmount(new BigDecimal("1000.00"))
                .active(true)
                .startDate(LocalDate.now().minusDays(1))
                .expiryDate(LocalDate.now().plusDays(1))
                .build();

        when(couponRepository.findByCodeIgnoreCase("MIN1000")).thenReturn(Optional.of(coupon));

        ApiException exception = assertThrows(ApiException.class, () ->
                couponService.validateAndComputeDiscount("MIN1000", new BigDecimal("850.00"), testUser)
        );
        assertTrue(exception.getMessage().contains("Minimum order amount"));
    }

    @Test
    @DisplayName("Should reject expired coupon")
    void testExpiredCouponRejection() {
        Coupon coupon = Coupon.builder()
                .code("EXPIRED")
                .type(CouponType.FIXED)
                .value(new BigDecimal("50.00"))
                .minOrderAmount(BigDecimal.ZERO)
                .active(true)
                .startDate(LocalDate.now().minusDays(10))
                .expiryDate(LocalDate.now().minusDays(1))
                .build();

        when(couponRepository.findByCodeIgnoreCase("EXPIRED")).thenReturn(Optional.of(coupon));

        ApiException exception = assertThrows(ApiException.class, () ->
                couponService.validateAndComputeDiscount("EXPIRED", new BigDecimal("500.00"), testUser)
        );
        assertTrue(exception.getMessage().contains("not valid today"));
    }

    @Test
    @DisplayName("Should reject coupon when per-user limit is exceeded")
    void testPerUserLimitRejection() {
        Coupon coupon = Coupon.builder()
                .code("ONCEONLY")
                .type(CouponType.FIXED)
                .value(new BigDecimal("50.00"))
                .minOrderAmount(BigDecimal.ZERO)
                .perUserLimit(1)
                .active(true)
                .startDate(LocalDate.now().minusDays(1))
                .expiryDate(LocalDate.now().plusDays(5))
                .build();

        when(couponRepository.findByCodeIgnoreCase("ONCEONLY")).thenReturn(Optional.of(coupon));
        when(orderRepository.countByUserAndCouponCode(testUser, "ONCEONLY")).thenReturn(1L);

        ApiException exception = assertThrows(ApiException.class, () ->
                couponService.validateAndComputeDiscount("ONCEONLY", new BigDecimal("500.00"), testUser)
        );
        assertTrue(exception.getMessage().contains("maximum number of times"));
    }
}
