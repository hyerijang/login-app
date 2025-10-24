package com.koreanmarkers.assignment.login_app.service;

import com.koreanmarkers.assignment.login_app.domain.AuthProvider;
import com.koreanmarkers.assignment.login_app.domain.User;
import com.koreanmarkers.assignment.login_app.domain.UserDetail;
import com.koreanmarkers.assignment.login_app.domain.UserRole;
import com.koreanmarkers.assignment.login_app.domain.UserSocial;
import com.koreanmarkers.assignment.login_app.repository.UserAuthRepository;
import com.koreanmarkers.assignment.login_app.repository.UserRepository;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = mapProvider(registrationId);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String providerId = extractProviderId(provider, attributes);
        String name = extractName(provider, attributes);
        String email = extractEmail(provider, attributes);
        String picture = extractPicture(provider, attributes);

        User user = userRepository.findByAuthProviders_ProviderAndAuthProviders_ProviderId(provider, providerId)
            .orElseGet(() -> {
                User newUser = User.builder()
                    .nickname(name)
                    .pictureUrl(picture)
                    .role(UserRole.USER)
                    .build();

                UserDetail newDetail = UserDetail.builder()
                    .name(name)
                    .email(email)
                    .build();

                UserSocial newUserSocial = UserSocial.builder()
                    .provider(provider)
                    .providerId(providerId)
                    .user(newUser)
                    .build();

                // 연관관계 설정
                newUser.setUserDetail(newDetail);
                newUser.addAuthProvider(newUserSocial);
                return newUser;
            });

        userRepository.save(user);

        return new DefaultOAuth2User(
            Collections.singleton(new SimpleGrantedAuthority(user.getRole().getKey())),
            attributes,
            getNameAttributeKey(provider)
        );
    }

    private AuthProvider mapProvider(String registrationId) {
        if ("google".equalsIgnoreCase(registrationId)) {
            return AuthProvider.GOOGLE;
        }
        throw new IllegalArgumentException("Unsupported provider: " + registrationId);
    }

    private String getNameAttributeKey(AuthProvider provider) {
        if (provider == AuthProvider.GOOGLE) {
            return "sub";
        }
        return "id";
    }

    private String extractProviderId(AuthProvider provider, Map<String, Object> attributes) {
        if (provider == AuthProvider.GOOGLE) {
            return String.valueOf(attributes.get("sub"));
        }
        return null;
    }

    private String extractName(AuthProvider provider, Map<String, Object> attributes) {
        if (provider == AuthProvider.GOOGLE) {
            return (String) attributes.getOrDefault("name", attributes.get("given_name"));
        }
        return null;
    }

    private String extractEmail(AuthProvider provider, Map<String, Object> attributes) {
        if (provider == AuthProvider.GOOGLE) {
            return (String) attributes.get("email");
        }
        return null;
    }

    private String extractPicture(AuthProvider provider, Map<String, Object> attributes) {
        if (provider == AuthProvider.GOOGLE) {
            return (String) attributes.get("picture");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cast(Object obj) {
        return (Map<String, Object>) obj;
    }
}