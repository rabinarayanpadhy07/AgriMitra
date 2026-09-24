package com.shopeasy.auth.service;

import com.shopeasy.auth.dto.CategoryRequest;
import com.shopeasy.auth.dto.CategoryResponse;
import com.shopeasy.auth.dto.admin.PageResponse;
import com.shopeasy.auth.entity.Category;
import com.shopeasy.auth.exception.ApiException;
import com.shopeasy.auth.exception.ResourceNotFoundException;
import com.shopeasy.auth.repository.CategoryRepository;
import com.shopeasy.auth.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> listActive() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> listAll(String search, Pageable pageable) {
        Specification<Category> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<CategoryResponse> page = categoryRepository.findAll(spec, pageable).map(CategoryResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return CategoryResponse.from(findById(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Category parent = request.getParentCategoryId() != null ? findById(request.getParentCategoryId()) : null;

        Category category = Category.builder()
                .name(request.getName().trim())
                .slug(generateUniqueSlug(request.getName()))
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .parentCategory(parent)
                .active(request.getActive() == null || request.getActive())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findById(id);
        Category parent = request.getParentCategoryId() != null ? findById(request.getParentCategoryId()) : null;

        if (parent != null && parent.getId().equals(id)) {
            throw new ApiException("A category cannot be its own parent");
        }

        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        category.setParentCategory(parent);
        if (request.getActive() != null) category.setActive(request.getActive());
        if (request.getDisplayOrder() != null) category.setDisplayOrder(request.getDisplayOrder());

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = findById(id);

        long productCount = productRepository.countByCategory(category);
        if (productCount > 0) {
            throw new ApiException("Cannot delete category with " + productCount + " product(s) assigned. Reassign or remove them first.");
        }
        long subCategoryCount = categoryRepository.countByParentCategory(category);
        if (subCategoryCount > 0) {
            throw new ApiException("Cannot delete category with subcategories. Remove or reassign them first.");
        }

        categoryRepository.delete(category);
    }

    @Transactional
    public CategoryResponse setActive(Long id, boolean active) {
        Category category = findById(id);
        category.setActive(active);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    private Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private String generateUniqueSlug(String name) {
        String base = name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "category";
        String slug = base;
        int suffix = 1;
        while (categoryRepository.existsBySlug(slug)) {
            slug = base + "-" + (++suffix);
        }
        return slug;
    }
}
