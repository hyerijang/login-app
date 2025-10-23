package com.koreanmarkers.assignment.login_app.service;

import com.koreanmarkers.assignment.login_app.domain.AuthProvider;
import com.koreanmarkers.assignment.login_app.domain.User;
import com.koreanmarkers.assignment.login_app.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.refresh-token.expiration-ms}")
    private long refreshTokenExpirationMs;

    // 개발 환경에서 HTTPS를 쓰지 않을 경우 Secure 쿠키를 끌 수 있게 토글
    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            response.sendRedirect("/");
            return;
        }

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        AuthProvider provider = mapProvider(registrationId);
        Map<String, Object> attributes = oauthUser.getAttributes();
        String providerId = extractProviderId(provider, attributes);

        Optional<User> userOpt = userRepository.findByAuthProviders_ProviderAndAuthProviders_ProviderId(provider, providerId);
        if (userOpt.isEmpty()) {
            response.sendRedirect("/?error=user_not_found");
            return;
        }

        User user = userOpt.get();
        // 액세스 토큰 생성 (JWT)
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getRole());

        // 리프레시 토큰 생성 및 DB 저장 (raw 토큰을 클라이언트에 전달, 해시는 DB에 보관됨)
        String refreshTokenRaw = refreshTokenService.createRefreshToken(user);

        // 쿠키로 전달: HttpOnly, Path=/, SameSite=Lax, Secure은 설정으로 제어
        int accessMaxAge = (int) (jwtExpirationMs / 1000);
        int refreshMaxAge = (int) (refreshTokenExpirationMs / 1000);

        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .maxAge(accessMaxAge)
            .sameSite("Lax")
            .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshTokenRaw)
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .maxAge(refreshMaxAge)
            .sameSite("Lax")
            .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

        // 리다이렉트 (토큰은 더 이상 URL 파라미터로 전달하지 않음)
        response.sendRedirect("/");
    }

    private AuthProvider mapProvider(String registrationId) {
        if ("google".equalsIgnoreCase(registrationId)) {
            return AuthProvider.GOOGLE;
        }
        throw new IllegalArgumentException("Unsupported provider: " + registrationId);
    }

    private String extractProviderId(AuthProvider provider, Map<String, Object> attributes) {
        if (provider == AuthProvider.GOOGLE) {
            return String.valueOf(attributes.get("sub"));
        }
        return null;
    }
}
