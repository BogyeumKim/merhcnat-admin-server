package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.SessionResult;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * 로그인 완료(200, code=SUCCESS) data. deviceState: NONE | MATCH | CHANGED.
 * 만료된 비밀번호는 로그인 완료 전에 변경을 강제하므로 passwordChangeRequired 는 호환용이며 보통 false 다.
 */
@JsonPropertyOrder({"userId", "accessToken", "loginType", "userType", "deviceState", "passwordChangeRequired", "passwordChangeSkippable", "passwordChangedAt", "deviceSecret"})
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthSessionResponse implements AuthStepResponse {

	private final String userId;

	/** Access-Token(JWT). 이후 요청의 {@code Authorization: Bearer <accessToken>} 으로 쓴다(헤더가 아니라 본문으로 내려준다). 앱은 SecureStore 에 보관한다 */
	private final String accessToken;

	private final @Nullable Integer loginType;

	private final @Nullable Integer userType;

	private final String deviceState;

	private final boolean passwordChangeRequired;

	private final boolean passwordChangeSkippable;

	private final @Nullable LocalDateTime passwordChangedAt;

	/**
	 * OTP·비밀번호 변경을 거쳐 기기를 등록했을 때만 한 번 내려가는 기기 비밀키. 앱은 SecureStore 에 보관하고
	 * 다음 로그인의 device.deviceSecret 으로 보낸다. 그 외에는 응답에서 제외
	 */
	private final @Nullable String deviceSecret;

	public static AuthSessionResponse from(SessionResult s, String accessToken) {
		String deviceState = s.getDeviceState().name();
		return AuthSessionResponse.with()
				.userId(s.getUserId())
				.accessToken(accessToken)
				.loginType(s.getLoginType())
				.userType(s.getUserType())
				.deviceState(deviceState)
				.passwordChangeRequired(s.isPasswordChangeRequired())
				.passwordChangeSkippable(false)
				.passwordChangedAt(s.getPasswordChangedAt())
				.deviceSecret(s.getDeviceSecret())
				.create();
	}
}
