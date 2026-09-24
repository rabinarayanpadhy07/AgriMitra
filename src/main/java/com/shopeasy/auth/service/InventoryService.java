package com.shopeasy.auth.service;

import com.shopeasy.auth.dto.InventoryAdjustRequest;
import com.shopeasy.auth.dto.InventoryTransactionResponse;
import com.shopeasy.auth.dto.ProductResponse;
import com.shopeasy.auth.dto.admin.PageResponse;
import com.shopeasy.auth.entity.*;
import com.shopeasy.auth.exception.ApiException;
import com.shopeasy.auth.exception.ResourceNotFoundException;
import com.shopeasy.auth.repository.InventoryTransactionRepository;
import com.shopeasy.auth.repository.ProductRepository;
import com.shopeasy.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> lowStockProducts(Pageable pageable) {
        return PageResponse.from(productRepository.findLowStockProducts(pageable).map(ProductResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> outOfStockProducts(Pageable pageable) {
        return PageResponse.from(productRepository.findOutOfStockProducts(pageable).map(ProductResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryTransactionResponse> listTransactions(Long productId, Pageable pageable) {
        Page<InventoryTransaction> page;
        if (productId != null) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
            page = inventoryTransactionRepository.findByProductOrderByCreatedAtDesc(product, pageable);
        } else {
            page = inventoryTransactionRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return PageResponse.from(page.map(InventoryTransactionResponse::from));
    }

    @Transactional
    public ProductResponse adjustStock(Long productId, InventoryAdjustRequest request, String performedByEmail) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        InventoryChangeType changeType;
        try {
            changeType = InventoryChangeType.valueOf(request.getChangeType().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid change type. Must be ADD, REMOVE, or ADJUST");
        }

        int quantity = request.getQuantity() != null ? request.getQuantity() : 0;
        int newStock;
        int delta;

        switch (changeType) {
            case ADD -> {
                newStock = product.getStock() + quantity;
                delta = quantity;
            }
            case REMOVE -> {
                if (quantity > product.getStock()) {
                    throw new ApiException("Cannot remove " + quantity + " units; only " + product.getStock() + " in stock");
                }
                newStock = product.getStock() - quantity;
                delta = -quantity;
            }
            case ADJUST -> {
                if (quantity < 0) {
                    throw new ApiException("Adjusted stock cannot be negative");
                }
                delta = quantity - product.getStock();
                newStock = quantity;
            }
            default -> throw new ApiException("Unsupported change type");
        }

        if (newStock < 0) {
            throw new ApiException("Stock cannot go negative");
        }

        product.setStock(newStock);
        productRepository.save(product);

        User performedBy = performedByEmail != null ? userRepository.findByEmail(performedByEmail).orElse(null) : null;

        InventoryTransaction transaction = InventoryTransaction.builder()
                .product(product)
                .changeType(changeType)
                .quantityChange(delta)
                .resultingStock(newStock)
                .reason(request.getReason())
                .performedBy(performedBy)
                .build();
        inventoryTransactionRepository.save(transaction);

        if (newStock <= 0) {
            notificationService.notifyAdmins(NotificationType.OUT_OF_STOCK,
                    "Product out of stock", product.getName() + " is now out of stock");
        } else if (newStock <= product.getLowStockThreshold()) {
            notificationService.notifyAdmins(NotificationType.LOW_STOCK,
                    "Low stock warning", product.getName() + " has only " + newStock + " units left");
        }

        return ProductResponse.from(product);
    }
}
