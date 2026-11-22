package com.app.demo.global.security;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Access-Token 검증으로 확인된 인증 사용자. 컨트롤러에서 {@code @AuthenticationPrincipal} 로 받는다.
 *
 * @param deviceIdHash 토큰을 발급받은 기기 ID 의 SHA-256 hex. 토큰에는 기기 ID 원문을 싣지 않는다
 * @param jti          토큰 고유 ID. 로그인 세션(Redis)의 jti 와 같아야 유효하다
 * @param expiresAt    토큰 만료 시각
 */
public record AuthUser(String userId, @Nullable String deviceIdHash, String jti, Instant expiresAt) {
}
