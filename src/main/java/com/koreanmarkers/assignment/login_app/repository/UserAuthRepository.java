package com.koreanmarkers.assignment.login_app.repository;

import com.koreanmarkers.assignment.login_app.domain.AuthProvider;
import com.koreanmarkers.assignment.login_app.domain.UserSocial;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAuthRepository extends JpaRepository<UserSocial, Long> {
    Optional<UserSocial> findByProviderAndProviderId(AuthProvider provider, String providerId);
}