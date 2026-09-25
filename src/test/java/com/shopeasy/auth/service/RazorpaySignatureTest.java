package com.shopeasy.auth.service;

import com.shopeasy.auth.dto.OrderResponse;
import com.shopeasy.auth.dto.RazorpayVerificationRequest;
import com.shopeasy.auth.entity.*;
import com.shopeasy.auth.exception.ApiException;
import com.shopeasy.auth.repository.OrderRepository;
import com.shopeasy.auth.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RazorpaySignatureTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RazorpayService razorpayService;

    private final String secret = "test_razorpay_secret_key_987";
    private User testUser;
    private Order testOrder;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(razorpayService, "keySecret", secret);

        testUser = User.builder()
                .id(1L)
                .email("farmer@agrimitra.com")
                .fullName("Ramesh Farmer")
                .build();

        testOrder = Order.builder()
                .id(101L)
                .user(testUser)
                .status(OrderStatus.PLACED)
                .totalAmount(new BigDecimal("1250.00"))
                .build();

        testPayment = Payment.builder()
                .id(201L)
                .order(testOrder)
                .user(testUser)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("1250.00"))
                .build();
    }

    private String computeHmacSha256(String orderId, String paymentId, String secretKey) throws Exception {
        String payload = orderId + "|" + paymentId;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    @Test
    @DisplayName("Should successfully verify valid Razorpay HMAC-SHA256 signature and confirm order")
    void testVerifyValidSignature() throws Exception {
        String rzpOrderId = "order_Oq78129abc";
        String rzpPaymentId = "pay_Pq98324xyz";
        String validSignature = computeHmacSha256(rzpOrderId, rzpPaymentId, secret);

        RazorpayVerificationRequest request = RazorpayVerificationRequest.builder()
                .orderId(101L)
                .razorpayOrderId(rzpOrderId)
                .razorpayPaymentId(rzpPaymentId)
                .razorpaySignature(validSignature)
                .build();

        when(orderRepository.findByIdAndUser(101L, testUser)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findByOrder(testOrder)).thenReturn(Optional.of(testPayment));
        when(orderService.myOrder(testUser, 101L)).thenReturn(OrderResponse.builder().id(101L).status("CONFIRMED").build());

        OrderResponse response = razorpayService.verifyPayment(testUser, request);

        assertNotNull(response);
        assertEquals(PaymentStatus.PAID, testPayment.getStatus());
        assertEquals(rzpPaymentId, testPayment.getTransactionRef());
        assertEquals(OrderStatus.CONFIRMED, testOrder.getStatus());
        verify(paymentRepository).save(testPayment);
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("Should reject tampered Razorpay signature and mark payment as FAILED")
    void testRejectTamperedSignature() {
        String rzpOrderId = "order_Oq78129abc";
        String rzpPaymentId = "pay_Pq98324xyz";
        String forgedSignature = "forged_invalid_signature_hash_12345";

        RazorpayVerificationRequest request = RazorpayVerificationRequest.builder()
                .orderId(101L)
                .razorpayOrderId(rzpOrderId)
                .razorpayPaymentId(rzpPaymentId)
                .razorpaySignature(forgedSignature)
                .build();

        when(orderRepository.findByIdAndUser(101L, testUser)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findByOrder(testOrder)).thenReturn(Optional.of(testPayment));

        ApiException exception = assertThrows(ApiException.class, () ->
                razorpayService.verifyPayment(testUser, request)
        );

        assertTrue(exception.getMessage().contains("invalid signature"));
        assertEquals(PaymentStatus.FAILED, testPayment.getStatus());
        verify(paymentRepository).save(testPayment);
        verify(orderRepository, never()).save(any(Order.class));
    }
}
