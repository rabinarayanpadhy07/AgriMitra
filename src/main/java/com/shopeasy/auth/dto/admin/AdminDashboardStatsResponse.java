package com.shopeasy.auth.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsResponse {

    private long totalUsers;
    private long totalSellers;
    private long totalAdmins;
    private long pendingSellerApplications;
    private long blockedUsers;
    private long newUsersToday;
    private long newUsersThisMonth;

    private long totalProducts;
    private long activeProducts;
    private long lowStockProducts;
    private long outOfStockProducts;

    private long totalOrders;
    private long pendingOrders;
    private long completedOrders;
    private long cancelledOrders;

    private BigDecimal totalRevenue;
    private BigDecimal todayRevenue;
    private BigDecimal monthlyRevenue;
}
