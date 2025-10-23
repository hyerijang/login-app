package com.koreanmarkers.assignment.login_app.service;

import com.koreanmarkers.assignment.login_app.repository.RefreshTokenRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomLogoutSuccessHandler.class);

    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {
            String refreshRaw = null;
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if ("refresh_token".equals(c.getName())) {
                        refreshRaw = c.getValue();
                        break;
                    }
                }
            }

            if (refreshRaw != null) {
                String tokenHash = refreshTokenService.hash(refreshRaw);
                Optional.ofNullable(refreshTokenRepository.findByTokenHash(tokenHash).orElse(null))
                    .ifPresent(rt -> {
                        refreshTokenRepository.delete(rt);
                        log.debug("Deleted refresh token from DB for hash={}", tokenHash);
                    });
            }
        } catch (Exception e) {
            log.warn("Error while deleting refresh token on logout: {}", e.toString());
        }

        // Clear cookies: access_token, refresh_token, JSESSIONID
        ResponseCookie clearAccess = ResponseCookie.from("access_token", "")
            .path("/")
            .httpOnly(true)
            .secure(cookieSecure)
            .maxAge(0)
            .sameSite("Lax")
            .build();

        ResponseCookie clearRefresh = ResponseCookie.from("refresh_token", "")
            .path("/")
            .httpOnly(true)
            .secure(cookieSecure)
            .maxAge(0)
            .sameSite("Lax")
            .build();

        ResponseCookie clearJsession = ResponseCookie.from("JSESSIONID", "")
            .path("/")
            .httpOnly(true)
            .secure(cookieSecure)
            .maxAge(0)
            .build();

        response.addHeader("Set-Cookie", clearAccess.toString());
        response.addHeader("Set-Cookie", clearRefresh.toString());
        response.addHeader("Set-Cookie", clearJsession.toString());

        // Invalidate session explicitly
        try {
            request.getSession(false);
            if (request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }
        } catch (Exception ignore) {}

        // Redirect to home
        response.sendRedirect("/");
    }
}
