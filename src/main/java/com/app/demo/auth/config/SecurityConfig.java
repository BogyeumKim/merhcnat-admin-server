package com.app.demo.auth.config;

import com.app.demo.auth.application.SessionService;
import com.app.demo.auth.domain.TokenProvider;
import com.app.demo.auth.infrastructure.JwtAuthFilter;
import com.app.demo.global.security.SecurityErrorHandler;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** 보안 설정: 무상태(JWT + Redis 로그인 세션), 로그인 경로(api/auth 하위)만 공개, 나머지는 Bearer 토큰 + 현재 세션 필요. */
@Configuration
class SecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http, TokenProvider tokens, SessionService sessions,
			SecurityErrorHandler securityErrorHandler)
			throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable) // 쿠키 세션을 쓰지 않는 API 서버
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(e -> e
						.authenticationEntryPoint(securityErrorHandler) // 토큰 없음/무효 → 401 ErrorResponse
						.accessDeniedHandler(securityErrorHandler))     // 권한 없음 → 403 ErrorResponse
				.authorizeHttpRequests(a -> a
						// 오류 페이지(/error) 내부 전달은 허용: 원래 요청이 이미 인증 검사를 거쳤고, 그래야 405/400 등이 실제 status 로 나간다.
						// (/error 를 직접 호출하는 REQUEST 는 여전히 인증이 필요하다)
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers("/api/auth/**").permitAll() // 로그인/OTP 단계
						.anyRequest().authenticated())
				.addFilterBefore(new JwtAuthFilter(tokens, sessions), UsernamePasswordAuthenticationFilter.class)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable);
		return http.build();
	}
}
