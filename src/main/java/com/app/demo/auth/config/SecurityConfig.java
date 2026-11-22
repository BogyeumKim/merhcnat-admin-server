package com.app.demo.auth.config;

import com.app.demo.auth.domain.TokenProvider;
import com.app.demo.auth.infrastructure.JwtAuthFilter;
import com.app.demo.global.security.SecurityErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** 보안 설정: 무상태(JWT), 로그인 경로(api/auth 하위)만 공개, 나머지는 Bearer 토큰 필요. */
@Configuration
class SecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http, TokenProvider tokens, SecurityErrorHandler securityErrorHandler)
			throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable) // 쿠키 세션을 쓰지 않는 API 서버
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(e -> e
						.authenticationEntryPoint(securityErrorHandler) // 토큰 없음/무효 → 401 공통 봉투
						.accessDeniedHandler(securityErrorHandler))     // 권한 없음 → 403 공통 봉투
				.authorizeHttpRequests(a -> a
						.requestMatchers("/api/auth/**").permitAll() // 로그인/OTP 단계
						.anyRequest().authenticated())
				.addFilterBefore(new JwtAuthFilter(tokens), UsernamePasswordAuthenticationFilter.class)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable);
		return http.build();
	}
}
