package com.app.demo.auth.domain;

import com.app.demo.global.security.AuthUser;
import com.app.demo.user.domain.DeviceInfo;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** 토큰 발급/검증 포트. access(로그인 완료 후)와 mfa(OTP 단계 전용)는 서로 대체될 수 없다. */
public interface TokenProvider {

	/** Access-Token 발급(토큰마다 새 jti). 토큰에는 deviceId 원문 대신 해시를 실어 "어느 기기의 토큰인지" 식별한다. */
	AccessToken createAccessToken(String userId, @Nullable Integer userType, String deviceId);

	/** Access-Token 검증. 서명/만료가 틀리거나 access 토큰이 아니거나 jti 가 없으면 empty. 세션 확인은 호출자가 한다. */
	Optional<AuthUser> parseAccessToken(String token);

	/** MFA 챌린지 토큰 발급. jti 는 user_info.mfa_jti 에 저장돼 일회용/최신 1개만 유효를 보장한다. ttlSeconds 는 절대 상한이다. */
	String createMfaToken(String userId, String jti, DeviceInfo device, long ttlSeconds);

	/** 임시비밀번호 변경 전용 챌린지 토큰 발급 (access/mfa 로 쓸 수 없다). jti 는 user_info.mfa_jti 로 일회용 보장. */
	String createPasswordChangeToken(String userId, String jti, DeviceInfo device, long ttlSeconds);

	/** 임시비밀번호 변경 토큰 검증. 서명/만료가 틀리거나 해당 종류가 아니면 empty. */
	Optional<MfaClaims> parsePasswordChangeToken(String token);

	/** MFA 챌린지 토큰 검증. 서명/만료가 틀리거나 mfa 토큰이 아니면 empty. */
	Optional<MfaClaims> parseMfaToken(String token);
}
