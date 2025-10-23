package com.koreanmarkers.assignment.login_app.config;

import com.koreanmarkers.assignment.login_app.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final CustomOAuth2UserService customOAuth2UserService;

	public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
		this.customOAuth2UserService = customOAuth2UserService;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/", "/css/**", "/js/**", "/images/**", "/h2-console/**").permitAll()
				.anyRequest().authenticated()
			)
			.oauth2Login(oauth -> oauth
				.userInfoEndpoint(userInfo -> userInfo
					.userService(customOAuth2UserService)
				)
			)
			.logout(logout -> logout
				.logoutUrl("/logout")
				.logoutSuccessUrl("/")
			)
			.headers(headers -> headers.frameOptions(frame -> frame.disable())); // H2 콘솔용

		return http.build();
	}
}