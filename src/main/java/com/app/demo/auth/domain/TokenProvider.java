package com.app.demo.auth.domain;

import com.app.demo.user.domain.DeviceInfo;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** 토큰 발급/검증 포트. access(로그인 완료 후)와 mfa(OTP 단계 전용)는 서로 대체될 수 없다. */
public interface TokenProvider {

	/** Access-Token 발급. deviceId 로 "어느 기기의 토큰인지" 식별한다. */
	String createAccessToken(String userId, @Nullable Integer userType, String deviceId);

	/** Access-Token 검증. 서명/만료가 틀리거나 access 토큰이 아니면 empty. */
	Optional<AuthUser> parseAccessToken(String token);

	/** MFA 챌린지 토큰 발급. jti 는 user_info.mfa_jti 에 저장돼 일회용/최신 1개만 유효를 보장한다. */
	String createMfaToken(String userId, String jti, DeviceInfo device, long ttlSeconds);

	/** MFA 챌린지 토큰 검증. 서명/만료가 틀리거나 mfa 토큰이 아니면 empty. */
	Optional<MfaClaims> parseMfaToken(String token);
}
