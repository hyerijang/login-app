package com.koreanmarkers.assignment.login_app.repository;

import com.koreanmarkers.assignment.login_app.domain.AuthProvider;
import com.koreanmarkers.assignment.login_app.domain.User;
import com.koreanmarkers.assignment.login_app.domain.UserSocial;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByAuthProvidersContains(UserSocial userSocial);

    Optional<User> findByAuthProviders_ProviderAndAuthProviders_ProviderId(AuthProvider provider, String providerId);
}




