package com.shopeasy.auth.service.admin;

import com.shopeasy.auth.dto.admin.AdminDashboardStatsResponse;
import com.shopeasy.auth.dto.admin.AdminUserSummaryResponse;
import com.shopeasy.auth.entity.AccountStatus;
import com.shopeasy.auth.entity.OrderStatus;
import com.shopeasy.auth.entity.SellerStatus;
import com.shopeasy.auth.entity.UserRole;
import com.shopeasy.auth.repository.OrderRepository;
import com.shopeasy.auth.repository.ProductRepository;
import com.shopeasy.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getStats() {
        LocalDateTime startOfToday = LocalDate.now(ZoneId.systemDefault()).atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now(ZoneId.systemDefault()).withDayOfMonth(1).atStartOfDay();

        long pendingOrders = orderRepository.countByStatus(OrderStatus.PLACED)
                + orderRepository.countByStatus(OrderStatus.CONFIRMED)
                + orderRepository.countByStatus(OrderStatus.PROCESSING)
                + orderRepository.countByStatus(OrderStatus.PACKED)
                + orderRepository.countByStatus(OrderStatus.SHIPPED)
                + orderRepository.countByStatus(OrderStatus.OUT_FOR_DELIVERY);

        BigDecimal totalRevenue = orderRepository.sumTotalRevenue();
        BigDecimal todayRevenue = orderRepository.sumRevenueSince(startOfToday);
        BigDecimal monthlyRevenue = orderRepository.sumRevenueSince(startOfMonth);

        return AdminDashboardStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalSellers(userRepository.countByRole(UserRole.SELLER))
                .totalAdmins(userRepository.countByRole(UserRole.ADMIN))
                .pendingSellerApplications(userRepository.countBySellerStatus(SellerStatus.PENDING))
                .blockedUsers(userRepository.countByAccountStatus(AccountStatus.BLOCKED))
                .newUsersToday(userRepository.countByCreatedAtAfter(startOfToday))
                .newUsersThisMonth(userRepository.countByCreatedAtAfter(startOfMonth))
                .totalProducts(productRepository.count())
                .activeProducts(productRepository.countByActiveTrue())
                .lowStockProducts(productRepository.countLowStockProducts())
                .outOfStockProducts(productRepository.countByStockAndActiveTrue(0))
                .totalOrders(orderRepository.count())
                .pendingOrders(pendingOrders)
                .completedOrders(orderRepository.countByStatus(OrderStatus.DELIVERED))
                .cancelledOrders(orderRepository.countByStatus(OrderStatus.CANCELLED))
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminUserSummaryResponse> getRecentUsers() {
        return userRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(AdminUserSummaryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AdminUserSummaryResponse> getRecentSellerApplications() {
        return userRepository.findTop5BySellerStatusOrderBySellerAppliedAtDesc(SellerStatus.PENDING).stream()
                .map(AdminUserSummaryResponse::from)
                .collect(Collectors.toList());
    }
}
