package com.shopeasy.auth.security;

import com.shopeasy.auth.entity.Session;
import com.shopeasy.auth.repository.SessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;
    private final SessionRepository sessionRepository;

    private static final long CACHE_TTL_MS = 60_000L; // 60 seconds TTL

    private record CachedSession(UserDetails userDetails, long expiresAt) {}
    private static final Map<String, CachedSession> SESSION_CACHE = new ConcurrentHashMap<>();

    public static void evictToken(String jwt) {
        if (jwt != null) {
            SESSION_CACHE.remove(jwt);
        }
    }

    public static void clearCache() {
        SESSION_CACHE.clear();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateToken(jwt)) {
                UserDetails userDetails = null;
                long now = System.currentTimeMillis();

                // 1. Fast in-memory cache lookup
                CachedSession cached = SESSION_CACHE.get(jwt);
                if (cached != null && now < cached.expiresAt()) {
                    userDetails = cached.userDetails();
                } else {
                    // 2. Fall back to database session verification
                    Optional<Session> sessionOpt = sessionRepository.findFirstByJwtTokenAndIsActiveTrueOrderByIdDesc(jwt);
                    if (sessionOpt.isPresent()) {
                        String email = jwtUtils.getEmailFromToken(jwt);
                        userDetails = userDetailsService.loadUserByUsername(email);
                        SESSION_CACHE.put(jwt, new CachedSession(userDetails, now + CACHE_TTL_MS));
                    } else {
                        SESSION_CACHE.remove(jwt);
                        logger.warn("Token presented is valid cryptographically, but session has been invalidated/logged out");
                    }
                }

                if (userDetails != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
