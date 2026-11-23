package com.app.demo.user.domain;

import com.app.demo.global.security.Sha256;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * 사용자 부가정보 애그리게이트(user 와 1:1). 인증 진행 상태와 등록 기기를 판단하는 데 필요한 값만 가진다.
 * (알림 설정 컬럼 등은 해당 기능이 생길 때 확장)
 */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserInfo {

	private final String userId;

	/** 현재 등록된 기기 (1 사용자 = 1 기기) */
	private final @Nullable String deviceId;

	/** 등록 기기 비밀키의 SHA-256 hex. 원문은 기기(앱 SecureStore)에만 있다 */
	private final @Nullable String deviceSecretHash;

	/** 마지막 사용된 TOTP time-step (재사용 방지) */
	private final @Nullable Long lastTotpStep;

	/** 현재 유효한 MFA 챌린지 토큰 ID. null 이면 유효한 챌린지 없음 */
	private final @Nullable String mfaJti;

	/** 현재 챌린지의 실제 유효 시각. MFA 챌린지는 OTP 발송 때마다 연장된다(토큰 만료가 절대 상한) */
	private final @Nullable LocalDateTime mfaExpiresAt;

	/** SMS/카카오 OTP 해시. 평문은 저장하지 않는다 */
	private final @Nullable String otpCodeHash;

	/** SMS | KAKAO | EMAIL */
	private final @Nullable String otpChannel;

	private final @Nullable LocalDateTime otpExpiresAt;

	/** 계정이 잠긴 시각 (잠금 화면 표시용) */
	private final @Nullable LocalDateTime lockedAt;

	/** 마지막으로 인증에 성공한 수단 (TOTP | KAKAO | SMS | EMAIL, "최근 사용" 표시용) */
	private final @Nullable String lastOtpChannel;

	/** 현재 MFA 챌린지에서 보낸 OTP 횟수(수단 합산, 새 챌린지 발급 시 0) */
	private final int otpSendCount;

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


	/** 현재 챌린지의 발송 횟수가 상한에 도달했는지 */
	public boolean hasReachedChallengeSendLimit(int maxSends) {
		return otpSendCount >= maxSends;
	}

	/** 현재 챌린지에서 더 보낼 수 있는 횟수 (0 이상) */
	public int remainingChallengeSends(int maxSends) {
		return Math.max(maxSends - otpSendCount, 0);
	}
}
