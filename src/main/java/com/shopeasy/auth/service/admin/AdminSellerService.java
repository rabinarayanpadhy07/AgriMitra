package com.shopeasy.auth.service.admin;

import com.shopeasy.auth.dto.admin.AdminUserDetailResponse;
import com.shopeasy.auth.dto.admin.AdminUserSummaryResponse;
import com.shopeasy.auth.dto.admin.PageResponse;
import com.shopeasy.auth.entity.SellerStatus;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.entity.UserRole;
import com.shopeasy.auth.exception.ApiException;
import com.shopeasy.auth.exception.ResourceNotFoundException;
import com.shopeasy.auth.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminSellerService {

    private static final Logger logger = LoggerFactory.getLogger(AdminSellerService.class);

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserSummaryResponse> listSellers(String search, SellerStatus sellerStatus,
                                                                Pageable pageable) {
        Specification<User> spec = buildSpecification(search, sellerStatus);
        Page<AdminUserSummaryResponse> page = userRepository.findAll(spec, pageable)
                .map(AdminUserSummaryResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getSellerById(Long id) {
        return AdminUserDetailResponse.from(findSellerById(id));
    }

    @Transactional
    public AdminUserDetailResponse approve(Long id) {
        User user = findSellerById(id);
        requireStatus(user, "approve", SellerStatus.PENDING, SellerStatus.SUSPENDED);

        user.setSellerStatus(SellerStatus.APPROVED);
        user.setRole(UserRole.SELLER);
        User saved = userRepository.save(user);
        logger.info("Admin approved seller application for: {}", saved.getEmail());

        return AdminUserDetailResponse.from(saved);
    }

    @Transactional
    public AdminUserDetailResponse reject(Long id) {
        User user = findSellerById(id);
        requireStatus(user, "reject", SellerStatus.PENDING);

        user.setSellerStatus(SellerStatus.REJECTED);
        User saved = userRepository.save(user);
        logger.info("Admin rejected seller application for: {}", saved.getEmail());

        return AdminUserDetailResponse.from(saved);
    }

    @Transactional
    public AdminUserDetailResponse suspend(Long id) {
        User user = findSellerById(id);
        requireStatus(user, "suspend", SellerStatus.APPROVED);

        user.setSellerStatus(SellerStatus.SUSPENDED);
        User saved = userRepository.save(user);
        logger.info("Admin suspended seller: {}", saved.getEmail());

        return AdminUserDetailResponse.from(saved);
    }

    @Transactional
    public AdminUserDetailResponse reactivate(Long id) {
        User user = findSellerById(id);
        requireStatus(user, "reactivate", SellerStatus.SUSPENDED);

        user.setSellerStatus(SellerStatus.APPROVED);
        User saved = userRepository.save(user);
        logger.info("Admin reactivated seller: {}", saved.getEmail());

        return AdminUserDetailResponse.from(saved);
    }

    private User findSellerById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        if (user.getSellerStatus() == null) {
            throw new ResourceNotFoundException("This user has no seller application on record");
        }
        return user;
    }

    private void requireStatus(User user, String action, SellerStatus... allowed) {
        for (SellerStatus status : allowed) {
            if (user.getSellerStatus() == status) {
                return;
            }
        }
        throw new ApiException("Cannot " + action + " seller from status " + user.getSellerStatus());
    }

    private Specification<User> buildSpecification(String search, SellerStatus sellerStatus) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNotNull(root.get("sellerStatus")));

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(root.get("mobileNumber"), pattern)
                ));
            }

            if (sellerStatus != null) {
                predicates.add(cb.equal(root.get("sellerStatus"), sellerStatus));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
