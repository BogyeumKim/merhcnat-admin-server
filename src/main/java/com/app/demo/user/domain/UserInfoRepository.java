package com.app.demo.user.domain;

import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/** 사용자 부가정보(user_info 테이블) 저장소 포트. 조건부 갱신은 동시 요청 경합을 막기 위해 boolean 으로 결과를 알린다. */
public interface UserInfoRepository {

	/** 없으면 기본값으로 생성하고 조회 */
	UserInfo findOrCreate(String userId);

	/** 해당 기기가 이미 어떤 사용자에게 등록돼 있는지 */
	boolean existsByDeviceId(String deviceId);

	/** 해당 기기가 등록된 사용자 아이디. 없으면 null */
	@Nullable String findUserIdByDeviceId(String deviceId);

	/** 계정 잠금 시각 기록 (잠금 화면 표시용) */
	void markLocked(String userId, LocalDateTime at);

	// ── MFA 챌린지 (일회용, 최신 1개만 유효) ──
	/** 새 챌린지 jti 와 유효 시각 저장 (이전 토큰은 자동 무효화, 챌린지당 발송 횟수 0 으로 초기화) */
	void issueChallenge(String userId, String jti, LocalDateTime expiresAt);

	/** jti 가 현재 값일 때만 소진. 소진했으면 true */
	boolean consumeMfa(String userId, String jti);

	// ── SMS/카카오 OTP (사용자당 1개, 해시만 저장) ──
	void saveOtp(String userId, String codeHash, String channel, LocalDateTime expiresAt, LocalDateTime sentAt);

	/**
	 * OTP 발송 1회를 원자적으로 예약한다. 아래 조건을 모두 만족할 때만 UPDATE 한 문장으로 발송 횟수를 +1 하고
	 * 발송 수단·시각을 기록하고 챌린지 유효 시각을 challengeExtendedUntil 로 연장한다(동시 요청이 몰려도 조건을 통과한 1건만 예약된다).
	 * <ul>
	 *   <li>챌린지(jti)가 그대로</li>
	 *   <li>챌린지당 상한 안 (수단 합산)</li>
	 * </ul>
	 *
	 * @return 예약에 성공하면 true, 하나라도 어긋나면 false
	 */
	boolean reserveOtpSend(String userId, String jti, String channel, int maxPerChallenge,
			LocalDateTime challengeExtendedUntil, LocalDateTime now);

	/** 저장된 해시가 그대로일 때만 비움(1회용). 성공하면 true */
	boolean consumeOtp(String userId, String codeHash);

	// ── TOTP / 로그인 ──
	void updateLastTotpStep(String userId, long step);

	/** 인증에 성공한 수단 기록 (다음 로그인의 "최근 사용" 표시용) */
	void updateLastOtpChannel(String userId, String channel);

	void markLogin(String userId, LocalDateTime at);

	// ── 기기 (1 사용자 = 1 기기 = 1 사용자) ──
	/**
	 * 기기를 등록한다. ① 이 기기를 쓰던 다른 사용자의 기기 정보(푸시 토큰·기기 비밀키 포함)를 비우고
	 * ② 이 사용자의 기기를 새 기기로 덮어쓴다. 같은 기기 재로그인이면 푸시 토큰/알림 허용은 유지한다.
	 *
	 * @param deviceSecretHash 새로 발급한 기기 비밀키의 해시. null 이면 기존 값을 유지한다(같은 기기 재로그인)
	 */
	void registerDevice(String userId, DeviceInfo device, @Nullable String deviceSecretHash, LocalDateTime now);

	/**
	 * 현재 등록 기기의 푸시 토큰 갱신(보낸 값만, 갱신 시각 기록). 같은 토큰이 다른 사용자에 남아 있으면 해제한다.
	 *
	 * @return 현재 등록 기기가 아니면 false
	 */
	boolean updatePush(String userId, String deviceId, @Nullable String expoPushToken, LocalDateTime now);
}
