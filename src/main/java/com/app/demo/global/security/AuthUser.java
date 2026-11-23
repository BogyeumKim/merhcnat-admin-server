package com.app.demo.global.security;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/** Access-Token 검증으로 확인된 인증 사용자. 컨트롤러에서 {@code @AuthenticationPrincipal} 로 받는다. */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthUser {

	private final String userId;

	/** 토큰을 발급받은 기기 ID 의 SHA-256 hex. 토큰에는 기기 ID 원문을 싣지 않는다 */
	private final @Nullable String deviceIdHash;

	/** 토큰 고유 ID. 로그인 세션(Redis)의 jti 와 같아야 유효하다 */
	private final String jti;

	/** 토큰 만료 시각 */
	private final Instant expiresAt;
}
