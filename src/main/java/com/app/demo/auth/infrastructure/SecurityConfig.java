package com.app.demo.auth.infrastructure;

import com.app.demo.auth.domain.TokenProvider;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** 보안 설정: 무상태(JWT), 로그인 경로(api/auth 하위)만 공개, 나머지는 Bearer 토큰 필요. */
@Configuration
class SecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http, TokenProvider tokens) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable) // 쿠키 세션을 쓰지 않는 API 서버
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
					// 인증 없이 보호된 경로 접근 → 공통 봉투 형식의 401
					res.setStatus(HttpStatus.UNAUTHORIZED.value());
					res.setContentType(MediaType.APPLICATION_JSON_VALUE);
					res.setCharacterEncoding(StandardCharsets.UTF_8.name());
					res.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\"}");
				}))
				.authorizeHttpRequests(a -> a
						.requestMatchers("/api/auth/**").permitAll() // 로그인/OTP 단계
						.anyRequest().authenticated())
				.addFilterBefore(new JwtAuthFilter(tokens), UsernamePasswordAuthenticationFilter.class)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable);
		return http.build();
	}
}
