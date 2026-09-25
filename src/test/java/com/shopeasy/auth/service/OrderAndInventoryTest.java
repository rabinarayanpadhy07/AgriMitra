package com.shopeasy.auth.service;

import com.shopeasy.auth.dto.CheckoutRequest;
import com.shopeasy.auth.dto.OrderResponse;
import com.shopeasy.auth.entity.*;
import com.shopeasy.auth.exception.ApiException;
import com.shopeasy.auth.exception.ResourceNotFoundException;
import com.shopeasy.auth.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderAndInventoryTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private CouponService couponService;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Address testAddress;
    private Cart testCart;
    private Product testProduct;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("kisan@agrimitra.com")
                .fullName("Kisan Mitra")
                .build();

        testAddress = Address.builder()
                .id(10L)
                .user(testUser)
                .fullName("Kisan Mitra")
                .phone("9876543210")
                .line1("Village Farm Plot 4")
                .city("Bhubaneswar")
                .state("Odisha")
                .pincode("751001")
                .country("India")
                .build();

        testCart = Cart.builder().id(5L).user(testUser).build();

        testProduct = Product.builder()
                .id(100L)
                .name("Organic Fertilizer 5kg")
                .price(new BigDecimal("300.00"))
                .discountPrice(new BigDecimal("250.00"))
                .stock(50)
                .active(true)
                .build();

        testCartItem = CartItem.builder()
                .id(501L)
                .cart(testCart)
                .product(testProduct)
                .quantity(2) // 2 * 250 = 500
                .build();
    }

    @Test
    @DisplayName("Checkout with subtotal >= 500 should qualify for FREE shipping (₹0)")
    void testCheckoutWithFreeShipping() {
        CheckoutRequest request = CheckoutRequest.builder()
                .addressId(10L)
                .paymentMethod("COD")
                .build();

        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCartOrderByCreatedAtDesc(testCart)).thenReturn(List.of(testCartItem));
        when(addressRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testAddress));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(999L);
            return o;
        });

        OrderResponse response = orderService.checkout(testUser, request);

        assertNotNull(response);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, atLeastOnce()).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();

        assertEquals(new BigDecimal("500.00"), savedOrder.getSubtotal());
        assertEquals(BigDecimal.ZERO, savedOrder.getShippingFee());
        assertEquals(new BigDecimal("500.00"), savedOrder.getTotalAmount());
        verify(cartItemRepository).deleteByCart(testCart);
    }

    @Test
    @DisplayName("Checkout with subtotal < 500 should add flat ₹50 shipping fee")
    void testCheckoutWithFlatShippingFee() {
        testCartItem.setQuantity(1); // 1 * 250 = 250 (< 500)

        CheckoutRequest request = CheckoutRequest.builder()
                .addressId(10L)
                .paymentMethod("ONLINE")
                .build();

        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCartOrderByCreatedAtDesc(testCart)).thenReturn(List.of(testCartItem));
        when(addressRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testAddress));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(999L);
            return o;
        });

        OrderResponse response = orderService.checkout(testUser, request);

        assertNotNull(response);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, atLeastOnce()).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();

        assertEquals(new BigDecimal("250.00"), savedOrder.getSubtotal());
        assertEquals(new BigDecimal("50.00"), savedOrder.getShippingFee());
        assertEquals(new BigDecimal("300.00"), savedOrder.getTotalAmount());
    }

    @Test
    @DisplayName("Checkout should fail if requested quantity exceeds product stock")
    void testCheckoutInsufficientStock() {
        testProduct.setStock(1);
        testCartItem.setQuantity(5); // 5 > 1

        CheckoutRequest request = CheckoutRequest.builder()
                .addressId(10L)
                .paymentMethod("COD")
                .build();

        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCartOrderByCreatedAtDesc(testCart)).thenReturn(List.of(testCartItem));
        when(addressRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testAddress));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

        ApiException exception = assertThrows(ApiException.class, () ->
                orderService.checkout(testUser, request)
        );

        assertTrue(exception.getMessage().contains("Insufficient stock"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin cancellation of an order must trigger restocking of inventory")
    void testAdminCancelRestocksInventory() {
        Order existingOrder = Order.builder()
                .id(888L)
                .status(OrderStatus.PLACED)
                .build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .order(existingOrder)
                .product(testProduct)
                .quantity(4)
                .build();

        when(orderRepository.findById(888L)).thenReturn(Optional.of(existingOrder));
        when(orderItemRepository.findByOrder(existingOrder)).thenReturn(List.of(item));
        when(orderRepository.save(any(Order.class))).thenReturn(existingOrder);

        orderService.adminCancel(888L);

        assertEquals(OrderStatus.CANCELLED, existingOrder.getStatus());
        verify(inventoryService).adjustStock(eq(testProduct.getId()), any(), isNull());
    }

    @Test
    @DisplayName("User ownership check: User cannot view another user's order")
    void testUserCannotViewAnotherUsersOrder() {
        when(orderRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                orderService.myOrder(testUser, 999L)
        );
    }
}
