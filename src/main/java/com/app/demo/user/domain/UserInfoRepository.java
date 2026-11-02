package com.app.demo.user.domain;

import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/** 사용자 부가정보(user_info 테이블) 저장소 포트. 조건부 갱신은 동시 요청 경합을 막기 위해 boolean 으로 결과를 알린다. */
public interface UserInfoRepository {

	/** 없으면 기본값으로 생성하고 조회 */
	UserInfo findOrCreate(String userId);

	/** 해당 기기가 이미 어떤 사용자에게 등록돼 있는지 */
	boolean existsByDeviceId(String deviceId);

	// ── MFA 챌린지 (일회용, 최신 1개만 유효) ──
	/** 새 챌린지 jti 저장 (이전 토큰은 자동 무효화) */
	void issueMfa(String userId, String jti);

	/** jti 가 현재 값일 때만 소진. 소진했으면 true */
	boolean consumeMfa(String userId, String jti);

	// ── SMS/카카오 OTP (사용자당 1개, 해시만 저장) ──
	void saveOtp(String userId, String codeHash, String channel, LocalDateTime expiresAt, LocalDateTime sentAt);

	/** 저장된 해시가 그대로일 때만 비움(1회용). 성공하면 true */
	boolean consumeOtp(String userId, String codeHash);

	// ── TOTP / 로그인 ──
	void updateLastTotpStep(String userId, long step);

	void markLogin(String userId, LocalDateTime at);

	// ── 기기 (1 사용자 = 1 기기 = 1 사용자) ──
	/**
	 * 기기를 등록한다. ① 이 기기를 쓰던 다른 사용자의 기기 정보(푸시 토큰 포함)를 비우고
	 * ② 이 사용자의 기기를 새 기기로 덮어쓴다. 같은 기기 재로그인이면 푸시 토큰/알림 허용은 유지한다.
	 */
	void registerDevice(String userId, DeviceInfo device, LocalDateTime now);

	/**
	 * 현재 등록 기기의 푸시 토큰 갱신(보낸 값만, 갱신 시각 기록). 같은 토큰이 다른 사용자에 남아 있으면 해제한다.
	 *
	 * @return 현재 등록 기기가 아니면 false
	 */
	boolean updatePush(String userId, String deviceId, @Nullable String expoPushToken, LocalDateTime now);
}
