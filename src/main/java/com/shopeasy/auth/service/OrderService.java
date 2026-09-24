package com.shopeasy.auth.service;

import com.shopeasy.auth.dto.*;
import com.shopeasy.auth.dto.admin.PageResponse;
import com.shopeasy.auth.entity.*;
import com.shopeasy.auth.exception.ApiException;
import com.shopeasy.auth.exception.ResourceNotFoundException;
import com.shopeasy.auth.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Set<OrderStatus> CANCELLABLE_BY_USER = Set.of(OrderStatus.PLACED, OrderStatus.CONFIRMED);
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = BigDecimal.valueOf(500);
    private static final BigDecimal FLAT_SHIPPING_FEE = BigDecimal.valueOf(50);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final CouponService couponService;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;

    @Transactional
    public OrderResponse checkout(User user, CheckoutRequest request) {
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ApiException("Your cart is empty"));
        List<CartItem> cartItems = cartItemRepository.findByCartOrderByCreatedAtDesc(cart);
        if (cartItems.isEmpty()) {
            throw new ApiException("Your cart is empty");
        }

        Address address = addressRepository.findByIdAndUser(request.getAddressId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(request.getPaymentMethod().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid payment method");
        }

        // Recompute everything server-side from the live Product rows -- never trust client prices.
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product no longer available"));
            if (!product.getActive() || item.getQuantity() > product.getStock()) {
                throw new ApiException("Insufficient stock for " + product.getName() + ". Please update your cart.");
            }
            BigDecimal unitPrice = product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal discount = BigDecimal.ZERO;
        String couponCode = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            discount = couponService.validateAndComputeDiscount(request.getCouponCode(), subtotal, user);
            couponCode = request.getCouponCode().trim().toUpperCase();
        }

        BigDecimal afterDiscount = subtotal.subtract(discount);
        BigDecimal shippingFee = afterDiscount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : FLAT_SHIPPING_FEE;
        BigDecimal total = afterDiscount.add(shippingFee);

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PLACED)
                .subtotal(subtotal)
                .discountAmount(discount)
                .shippingFee(shippingFee)
                .totalAmount(total)
                .couponCode(couponCode)
                .shippingFullName(address.getFullName())
                .shippingPhone(address.getPhone())
                .shippingLine1(address.getLine1())
                .shippingLine2(address.getLine2())
                .shippingCity(address.getCity())
                .shippingState(address.getState())
                .shippingPincode(address.getPincode())
                .shippingCountry(address.getCountry())
                .build();
        order = orderRepository.save(order);

        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            BigDecimal unitPrice = product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .productImage(product.getImageUrls() != null && !product.getImageUrls().isEmpty() ? product.getImageUrls().get(0) : null)
                    .unitPrice(unitPrice)
                    .quantity(item.getQuantity())
                    .lineTotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                    .build();
            orderItemRepository.save(orderItem);

            InventoryAdjustRequest stockOut = InventoryAdjustRequest.builder()
                    .changeType("REMOVE")
                    .quantity(item.getQuantity())
                    .reason("Order #" + order.getId())
                    .build();
            inventoryService.adjustStock(product.getId(), stockOut, user.getEmail());
        }

        Payment payment = Payment.builder()
                .order(order)
                .user(user)
                .method(method)
                .status(PaymentStatus.PENDING)
                .amount(total)
                .build();
        paymentRepository.save(payment);

        if (couponCode != null) {
            couponService.recordUsage(couponCode);
        }

        cartItemRepository.deleteByCart(cart);

        notificationService.notifyAdmins(NotificationType.NEW_ORDER,
                "New order placed", "Order #" + order.getId() + " placed by " + user.getFullName() + " for " + total);

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> myOrders(User user, Pageable pageable) {
        Page<OrderResponse> page = orderRepository.findByUserOrderByCreatedAtDesc(user, pageable).map(this::toResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public OrderResponse myOrder(User user, Long id) {
        Order order = orderRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelMyOrder(User user, Long id) {
        Order order = orderRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!CANCELLABLE_BY_USER.contains(order.getStatus())) {
            throw new ApiException("This order can no longer be cancelled");
        }

        restock(order);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> adminList(String status, Pageable pageable) {
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), OrderStatus.valueOf(status.trim().toUpperCase())));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<OrderResponse> page = orderRepository.findAll(spec, pageable).map(this::toResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public OrderResponse adminGetById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public OrderResponse adminUpdateStatus(Long id, String statusValue) {
        Order order = findById(id);
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(statusValue.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid order status: " + statusValue);
        }

        if (newStatus == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {
            restock(order);
        }

        order.setStatus(newStatus);
        orderRepository.save(order);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse adminCancel(Long id) {
        return adminUpdateStatus(id, OrderStatus.CANCELLED.name());
    }

    private void restock(Order order) {
        for (OrderItem item : orderItemRepository.findByOrder(order)) {
            if (item.getProduct() == null) continue;
            InventoryAdjustRequest stockIn = InventoryAdjustRequest.builder()
                    .changeType("ADD")
                    .quantity(item.getQuantity())
                    .reason("Cancelled order #" + order.getId())
                    .build();
            inventoryService.adjustStock(item.getProduct().getId(), stockIn, null);
        }
    }

    Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.Item> items = orderItemRepository.findByOrder(order).stream()
                .map(i -> OrderResponse.Item.builder()
                        .id(i.getId())
                        .productId(i.getProduct() != null ? i.getProduct().getId() : null)
                        .productName(i.getProductName())
                        .productImage(i.getProductImage())
                        .unitPrice(i.getUnitPrice())
                        .quantity(i.getQuantity())
                        .lineTotal(i.getLineTotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .userName(order.getUser().getFullName())
                .userEmail(order.getUser().getEmail())
                .status(order.getStatus().name())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .couponCode(order.getCouponCode())
                .shippingFullName(order.getShippingFullName())
                .shippingPhone(order.getShippingPhone())
                .shippingLine1(order.getShippingLine1())
                .shippingLine2(order.getShippingLine2())
                .shippingCity(order.getShippingCity())
                .shippingState(order.getShippingState())
                .shippingPincode(order.getShippingPincode())
                .shippingCountry(order.getShippingCountry())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
