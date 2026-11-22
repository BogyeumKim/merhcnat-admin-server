package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.SessionResult;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/**
 * 로그인 완료(200) data. 토큰은 Access-Token 응답 헤더로 내려간다. deviceState: NONE | MATCH | CHANGED.
 * passwordChangeRequired 가 true 면 앱은 비밀번호 변경 화면을 띄운다(건너뛰기 불가, passwordChangeSkippable=false).
 */
public record AuthSessionResponse(String userId, @Nullable Integer loginType, @Nullable Integer userType, String deviceState,
		boolean passwordChangeRequired, boolean passwordChangeSkippable, @Nullable LocalDateTime passwordChangedAt) {

	public static AuthSessionResponse from(SessionResult s) {
		return new AuthSessionResponse(s.userId(), s.loginType(), s.userType(), s.deviceState().name(),
				s.passwordChangeRequired(), false, s.passwordChangedAt());
	}
}
