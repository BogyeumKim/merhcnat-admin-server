package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.SessionResult;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/**
 * 로그인 완료(200, code=SUCCESS) data. 토큰은 Access-Token 응답 헤더로 내려간다. deviceState: NONE | MATCH | CHANGED.
 * 만료된 비밀번호는 로그인 완료 전에 변경을 강제하므로 passwordChangeRequired 는 호환용이며 보통 false 다.
 *
 * @param deviceSecret OTP·비밀번호 변경을 거쳐 기기를 등록했을 때만 한 번 내려가는 기기 비밀키. 앱은 SecureStore 에 보관하고
 *                     다음 로그인의 device.deviceSecret 으로 보낸다. 그 외에는 응답에서 제외
 */
public record AuthSessionResponse(String userId, @Nullable Integer loginType, @Nullable Integer userType, String deviceState,
		boolean passwordChangeRequired, boolean passwordChangeSkippable, @Nullable LocalDateTime passwordChangedAt,
		@Nullable String deviceSecret) implements AuthStepResponse {

	public static AuthSessionResponse from(SessionResult s) {
		return new AuthSessionResponse(s.userId(), s.loginType(), s.userType(), s.deviceState().name(),
				s.passwordChangeRequired(), false, s.passwordChangedAt(), s.deviceSecret());
	}
}
