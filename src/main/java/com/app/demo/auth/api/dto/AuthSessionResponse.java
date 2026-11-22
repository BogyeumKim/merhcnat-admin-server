package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.SessionResult;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/**
 * 로그인 완료(200, code=SUCCESS) data. deviceState: NONE | MATCH | CHANGED.
 * accessToken 은 이후 요청의 {@code Authorization: Bearer <accessToken>} 으로 쓴다(응답 헤더가 아니라 본문으로 내려준다).
 * 만료된 비밀번호는 로그인 완료 전에 변경을 강제하므로 passwordChangeRequired 는 호환용이며 보통 false 다.
 *
 * @param accessToken  Access-Token(JWT). 앱은 SecureStore 에 보관한다
 * @param deviceSecret OTP·비밀번호 변경을 거쳐 기기를 등록했을 때만 한 번 내려가는 기기 비밀키. 앱은 SecureStore 에 보관하고
 *                     다음 로그인의 device.deviceSecret 으로 보낸다. 그 외에는 응답에서 제외
 */
public record AuthSessionResponse(String userId, String accessToken, @Nullable Integer loginType, @Nullable Integer userType, String deviceState,
		boolean passwordChangeRequired, boolean passwordChangeSkippable, @Nullable LocalDateTime passwordChangedAt,
		@Nullable String deviceSecret) implements AuthStepResponse {

	public static AuthSessionResponse from(SessionResult s, String accessToken) {
		return new AuthSessionResponse(s.userId(), accessToken, s.loginType(), s.userType(), s.deviceState().name(),
				s.passwordChangeRequired(), false, s.passwordChangedAt(), s.deviceSecret());
	}
}
