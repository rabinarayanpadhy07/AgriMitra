package com.shopeasy.auth.service;

import com.shopeasy.auth.entity.Session;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.repository.SessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;

    @Transactional
    public Session createSession(User user, String jwtToken, LocalDateTime expiryTime, HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : "Unknown";

        Session session = Session.builder()
                .user(user)
                .jwtToken(jwtToken)
                .loginTime(LocalDateTime.now())
                .expiryTime(expiryTime)
                .isActive(true)
                .ipAddress(clientIp)
                .userAgent(userAgent != null && userAgent.length() > 250 ? userAgent.substring(0, 250) : userAgent)
                .build();

        return sessionRepository.save(session);
    }

    @Transactional
    public void invalidateSession(String jwtToken) {
        sessionRepository.invalidateSessionByToken(jwtToken);
    }

    @Transactional
    public void invalidateAllUserSessions(User user) {
        sessionRepository.invalidateAllUserSessions(user);
    }

    @Transactional(readOnly = true)
    public List<Session> getActiveSessions(User user) {
        return sessionRepository.findByUserAndIsActiveTrueOrderByLoginTimeDesc(user);
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
