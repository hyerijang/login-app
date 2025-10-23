package com.koreanmarkers.assignment.login_app.config;

import com.koreanmarkers.assignment.login_app.service.CustomOAuth2UserService;
import com.koreanmarkers.assignment.login_app.service.OAuth2AuthenticationSuccessHandler;
import com.koreanmarkers.assignment.login_app.service.JwtAuthenticationFilter;
import com.koreanmarkers.assignment.login_app.service.CustomLogoutSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final CustomOAuth2UserService customOAuth2UserService;
	private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final CustomLogoutSuccessHandler customLogoutSuccessHandler;

	public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler, JwtAuthenticationFilter jwtAuthenticationFilter, CustomLogoutSuccessHandler customLogoutSuccessHandler) {
		this.customOAuth2UserService = customOAuth2UserService;
		this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.customLogoutSuccessHandler = customLogoutSuccessHandler;
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
				.successHandler(oAuth2AuthenticationSuccessHandler)
			)
			.logout(logout -> logout
				.logoutUrl("/logout")
				.logoutSuccessHandler(customLogoutSuccessHandler)
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.headers(headers -> headers.frameOptions(frame -> frame.disable())); // H2 콘솔용

		return http.build();
	}
}