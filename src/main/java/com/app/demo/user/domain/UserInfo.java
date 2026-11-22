package com.app.demo.user.domain;

import com.app.demo.global.security.Sha256;
import java.time.Duration;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/**
 * 사용자 부가정보 애그리게이트(user 와 1:1). 인증 진행 상태와 등록 기기를 판단하는 데 필요한 값만 가진다.
 * (알림 설정 컬럼 등은 해당 기능이 생길 때 확장)
 *
 * @param deviceId       현재 등록된 기기 (1 사용자 = 1 기기)
 * @param lastTotpStep   마지막 사용된 TOTP time-step (재사용 방지)
 * @param deviceSecretHash 등록 기기 비밀키의 SHA-256 hex. 원문은 기기(앱 SecureStore)에만 있다
 * @param mfaJti         현재 유효한 MFA 챌린지 토큰 ID. null 이면 유효한 챌린지 없음
 * @param mfaExpiresAt   현재 챌린지의 실제 유효 시각. MFA 챌린지는 OTP 발송 때마다 연장된다(토큰 만료가 절대 상한)
 * @param otpCodeHash    SMS/카카오 OTP 해시. 평문은 저장하지 않는다
 * @param otpChannel     SMS | KAKAO | EMAIL
 * @param otpSentAt      마지막 OTP 발송 시각 (재발송 제한)
 * @param lockedAt       계정이 잠긴 시각 (잠금 화면 표시용)
 * @param lastOtpChannel 마지막으로 인증에 성공한 수단 (TOTP | KAKAO | SMS | EMAIL, "최근 사용" 표시용)
 * @param otpSendCount   현재 MFA 챌린지에서 보낸 OTP 횟수 (새 챌린지 발급 시 0)
 * @param otpWindowStartedAt 시간당 발송 상한을 세는 1시간 창의 시작 시각
 * @param otpWindowCount 현재 1시간 창에서 보낸 OTP 횟수
 */
public record UserInfo(
		String userId,
		@Nullable String deviceId,
		@Nullable String deviceSecretHash,
		@Nullable Long lastTotpStep,
		@Nullable String mfaJti,
		@Nullable LocalDateTime mfaExpiresAt,
		@Nullable String otpCodeHash,
		@Nullable String otpChannel,
		@Nullable LocalDateTime otpExpiresAt,
		@Nullable LocalDateTime otpSentAt,
		@Nullable LocalDateTime lockedAt,
		@Nullable String lastOtpChannel,
		int otpSendCount,
		@Nullable LocalDateTime otpWindowStartedAt,
		int otpWindowCount) {

	/** 시간당 발송 상한을 세는 창의 길이 */
	public static final Duration OTP_SEND_WINDOW = Duration.ofHours(1);

	/** 이 기기가 현재 등록된 기기인지 */
	public boolean hasDevice(String candidateDeviceId) {
		return candidateDeviceId.equals(deviceId);
	}

	/**
	 * 등록 기기로 인정할 수 있는지: 기기 ID 가 같고, 기기가 제시한 비밀키의 해시가 저장된 값과 같아야 한다(상수 시간 비교).
	 * 기기 ID 는 비밀이 아니므로 ID 만으로는 인정하지 않는다.
	 */
	public boolean isTrustedDevice(String candidateDeviceId, @Nullable String deviceSecret) {
		return hasDevice(candidateDeviceId) && Sha256.matches(deviceSecret, deviceSecretHash);
	}

	/** 주어진 jti 가 지금 유효한 챌린지인지 (이미 사용했거나, 더 새로운 로그인이 발급됐거나, 유효 시각이 지나면 false) */
	public boolean isCurrentChallenge(String jti, LocalDateTime now) {
		return jti.equals(mfaJti) && (mfaExpiresAt == null || !mfaExpiresAt.isBefore(now));
	}

	/** TOTP 코드(time-step)가 이미 사용된 것(또는 이전 것)인지 */
	public boolean isTotpReplay(long step) {
		return lastTotpStep != null && step <= lastTotpStep;
	}

	/** 저장된 OTP 가 해당 채널로 발급돼 아직 만료되지 않았는지 */
	public boolean hasValidOtp(String channel, LocalDateTime now) {
		return otpCodeHash != null && channel.equals(otpChannel) && otpExpiresAt != null && !otpExpiresAt.isBefore(now);
	}

	/** 같은 수단으로 재발송하기까지 남은 대기 초. 다른 수단이거나 0 이하이면 즉시 발송 가능 */
	public long resendWaitSeconds(String channel, LocalDateTime now, long intervalSeconds) {
		if (otpSentAt == null || !channel.equals(otpChannel)) {
			return 0;
		}
		return intervalSeconds - Duration.between(otpSentAt, now).toSeconds();
	}

	/** 현재 챌린지의 발송 횟수가 상한에 도달했는지 */
	public boolean hasReachedChallengeSendLimit(int maxSends) {
		return otpSendCount >= maxSends;
	}

	/** 시간당 발송 상한에 걸렸다면 창이 끝날 때까지 남은 초, 아니면 0 */
	public long hourlySendLimitWaitSeconds(LocalDateTime now, int maxSends) {
		if (otpWindowStartedAt == null || otpWindowCount < maxSends) {
			return 0;
		}
		long remaining = Duration.between(now, otpWindowStartedAt.plus(OTP_SEND_WINDOW)).toSeconds();
		return Math.max(remaining, 0);
	}
}
