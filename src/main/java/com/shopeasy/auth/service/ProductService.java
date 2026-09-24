package com.shopeasy.auth.service;

import com.shopeasy.auth.dto.ProductRequest;
import com.shopeasy.auth.dto.ProductResponse;
import com.shopeasy.auth.dto.admin.PageResponse;
import com.shopeasy.auth.entity.Category;
import com.shopeasy.auth.entity.Product;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.exception.ApiException;
import com.shopeasy.auth.exception.ResourceNotFoundException;
import com.shopeasy.auth.repository.CategoryRepository;
import com.shopeasy.auth.repository.ProductRepository;
import com.shopeasy.auth.repository.ReviewRepository;
import com.shopeasy.auth.repository.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> publicList(String search, Long categoryId, BigDecimal minPrice,
                                                      BigDecimal maxPrice, Boolean featured, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Join<Product, Category> categoryJoin = root.join("category", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("name"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("crop"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("cropType"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("variety"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("manufacturer"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(categoryJoin.get("name"), "")), pattern)
                ));
            }
            if (categoryId != null) predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            if (featured != null) predicates.add(cb.equal(root.get("featured"), featured));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<ProductResponse> page = productRepository.findAll(spec, pageable).map(this::toResponseWithRating);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> adminList(String search, Long categoryId, Long sellerId, Boolean active,
                                                     Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("sku")), pattern)
                ));
            }
            if (categoryId != null) predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            if (sellerId != null) predicates.add(cb.equal(root.get("seller").get("id"), sellerId));
            if (active != null) predicates.add(cb.equal(root.get("active"), active));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<ProductResponse> page = productRepository.findAll(spec, pageable).map(this::toResponseWithRating);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return toResponseWithRating(findById(id));
    }

    @Transactional(readOnly = true)
    public ProductResponse getBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + slug));
        return toResponseWithRating(product);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku().trim())) {
            throw new ApiException("SKU already exists: " + request.getSku());
        }

        Product product = Product.builder()
                .name(request.getName().trim())
                .slug(generateUniqueSlug(request.getName()))
                .description(request.getDescription())
                .sku(request.getSku().trim())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .stock(request.getStock() != null ? request.getStock() : 0)
                .lowStockThreshold(request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 5)
                .active(request.getActive() == null || request.getActive())
                .featured(request.getFeatured() != null && request.getFeatured())
                .imageUrls(request.getImageUrls() != null ? request.getImageUrls() : new ArrayList<>())
                .category(request.getCategoryId() != null ? findCategory(request.getCategoryId()) : null)
                .seller(request.getSellerId() != null ? findUser(request.getSellerId()) : null)
                .crop(request.getCrop())
                .cropType(request.getCropType())
                .variety(request.getVariety())
                .season(request.getSeason())
                .soilType(request.getSoilType())
                .growingDuration(request.getGrowingDuration())
                .usageInstructions(request.getUsageInstructions())
                .dosage(request.getDosage())
                .composition(request.getComposition())
                .manufacturer(request.getManufacturer())
                .suitableRegion(request.getSuitableRegion())
                .build();

        return toResponseWithRating(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findById(id);

        if (!product.getSku().equals(request.getSku().trim()) && productRepository.existsBySku(request.getSku().trim())) {
            throw new ApiException("SKU already exists: " + request.getSku());
        }

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setSku(request.getSku().trim());
        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        if (request.getLowStockThreshold() != null) product.setLowStockThreshold(request.getLowStockThreshold());
        if (request.getActive() != null) product.setActive(request.getActive());
        if (request.getFeatured() != null) product.setFeatured(request.getFeatured());
        if (request.getImageUrls() != null) product.setImageUrls(request.getImageUrls());
        product.setCategory(request.getCategoryId() != null ? findCategory(request.getCategoryId()) : null);
        product.setSeller(request.getSellerId() != null ? findUser(request.getSellerId()) : null);
        product.setCrop(request.getCrop());
        product.setCropType(request.getCropType());
        product.setVariety(request.getVariety());
        product.setSeason(request.getSeason());
        product.setSoilType(request.getSoilType());
        product.setGrowingDuration(request.getGrowingDuration());
        product.setUsageInstructions(request.getUsageInstructions());
        product.setDosage(request.getDosage());
        product.setComposition(request.getComposition());
        product.setManufacturer(request.getManufacturer());
        product.setSuitableRegion(request.getSuitableRegion());

        return toResponseWithRating(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        productRepository.delete(findById(id));
    }

    @Transactional
    public ProductResponse setActive(Long id, boolean active) {
        Product product = findById(id);
        product.setActive(active);
        return toResponseWithRating(productRepository.save(product));
    }

    @Transactional
    public ProductResponse setFeatured(Long id, boolean featured) {
        Product product = findById(id);
        product.setFeatured(featured);
        return toResponseWithRating(productRepository.save(product));
    }

    Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private ProductResponse toResponseWithRating(Product product) {
        ProductResponse response = ProductResponse.from(product);
        Double avg = reviewRepository.averageRatingForProduct(product);
        response.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : null);
        response.setReviewCount(reviewRepository.countApprovedForProduct(product));
        return response;
    }

    private String generateUniqueSlug(String name) {
        String base = name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "product";
        String slug = base;
        int suffix = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = base + "-" + (++suffix);
        }
        return slug;
    }
}
