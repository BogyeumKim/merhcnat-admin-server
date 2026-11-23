package com.app.demo.auth.domain;

import com.app.demo.user.domain.DeviceInfo;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 챌린지 토큰에서 복원한 "진행 중인 로그인" 정보. */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MfaClaims {

	private final String userId;

	private final String jti;

	private final DeviceInfo device;

	/** 토큰 자체의 만료 시각(MFA 챌린지는 절대 상한). 실제 유효 시각은 user_info.mfa_expires_at 으로 관리한다 */
	private final LocalDateTime expiresAt;
}
