package com.app.demo.user.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/**
 * 사용자 부가정보 애그리게이트(user 와 1:1). 인증 진행 상태와 등록 기기를 판단하는 데 필요한 값만 가진다.
 * (알림 설정 컬럼 등은 해당 기능이 생길 때 확장)
 *
 * @param deviceId       현재 등록된 기기 (1 사용자 = 1 기기)
 * @param lastTotpStep   마지막 사용된 TOTP time-step (재사용 방지)
 * @param mfaJti         현재 유효한 MFA 챌린지 토큰 ID. null 이면 유효한 챌린지 없음
 * @param otpCodeHash    SMS/카카오 OTP 해시. 평문은 저장하지 않는다
 * @param otpChannel     SMS | KAKAO
 * @param otpSentAt      마지막 OTP 발송 시각 (재발송 제한)
 */
public record UserInfo(
		String userId,
		@Nullable String deviceId,
		@Nullable Long lastTotpStep,
		@Nullable String mfaJti,
		@Nullable String otpCodeHash,
		@Nullable String otpChannel,
		@Nullable LocalDateTime otpExpiresAt,
		@Nullable LocalDateTime otpSentAt) {

	/** 이 기기가 현재 등록된 기기인지 */
	public boolean hasDevice(String candidateDeviceId) {
		return candidateDeviceId.equals(deviceId);
	}

	/** 주어진 jti 가 현재 유효한 MFA 챌린지인지 (이미 사용했거나 더 새로운 로그인이 발급되면 false) */
	public boolean isCurrentMfa(String jti) {
		return jti.equals(mfaJti);
	}

	/** TOTP 코드(time-step)가 이미 사용된 것(또는 이전 것)인지 */
	public boolean isTotpReplay(long step) {
		return lastTotpStep != null && step <= lastTotpStep;
	}

	/** 저장된 OTP 가 해당 채널로 발급돼 아직 만료되지 않았는지 */
	public boolean hasValidOtp(String channel, LocalDateTime now) {
		return otpCodeHash != null && channel.equals(otpChannel) && otpExpiresAt != null && !otpExpiresAt.isBefore(now);
	}

	/** 재발송까지 남은 대기 초. 0 이하이면 즉시 재발송 가능 */
	public long resendWaitSeconds(LocalDateTime now, long intervalSeconds) {
		if (otpSentAt == null) {
			return 0;
		}
		return intervalSeconds - Duration.between(otpSentAt, now).getSeconds();
	}
}
