package com.shopeasy.auth.service.admin;

import com.shopeasy.auth.dto.admin.AdminUserDetailResponse;
import com.shopeasy.auth.dto.admin.AdminUserSummaryResponse;
import com.shopeasy.auth.dto.admin.PageResponse;
import com.shopeasy.auth.entity.AccountStatus;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.entity.UserRole;
import com.shopeasy.auth.exception.ApiException;
import com.shopeasy.auth.exception.ResourceNotFoundException;
import com.shopeasy.auth.repository.UserRepository;
import com.shopeasy.auth.service.SessionService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserService.class);

    private final UserRepository userRepository;
    private final SessionService sessionService;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserSummaryResponse> listUsers(String search, UserRole role,
                                                              AccountStatus accountStatus, Pageable pageable) {
        Specification<User> spec = buildSpecification(search, role, accountStatus);
        Page<AdminUserSummaryResponse> page = userRepository.findAll(spec, pageable)
                .map(AdminUserSummaryResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserById(Long id) {
        return AdminUserDetailResponse.from(findById(id));
    }

    @Transactional
    public AdminUserDetailResponse blockUser(Long id) {
        User user = findById(id);

        if (user.getAccountStatus() == AccountStatus.BLOCKED) {
            throw new ApiException("User is already blocked");
        }

        if (user.getRole() == UserRole.ADMIN
                && userRepository.countByRoleAndAccountStatus(UserRole.ADMIN, AccountStatus.ACTIVE) <= 1) {
            throw new ApiException("Cannot block the last active admin account", HttpStatus.CONFLICT);
        }

        user.setAccountStatus(AccountStatus.BLOCKED);
        User saved = userRepository.save(user);

        sessionService.invalidateAllUserSessions(saved);
        logger.info("Admin blocked user account: {}", saved.getEmail());

        return AdminUserDetailResponse.from(saved);
    }

    @Transactional
    public AdminUserDetailResponse unblockUser(Long id) {
        User user = findById(id);

        if (user.getAccountStatus() == AccountStatus.ACTIVE) {
            throw new ApiException("User is already active");
        }

        user.setAccountStatus(AccountStatus.ACTIVE);
        User saved = userRepository.save(user);
        logger.info("Admin unblocked user account: {}", saved.getEmail());

        return AdminUserDetailResponse.from(saved);
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private Specification<User> buildSpecification(String search, UserRole role, AccountStatus accountStatus) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(root.get("mobileNumber"), pattern)
                ));
            }

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            if (accountStatus != null) {
                predicates.add(cb.equal(root.get("accountStatus"), accountStatus));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
