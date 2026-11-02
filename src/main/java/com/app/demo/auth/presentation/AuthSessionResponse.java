package com.app.demo.auth.presentation;

import com.app.demo.auth.application.LoginResult;
import org.jspecify.annotations.Nullable;

/** 로그인 완료(200) data. 토큰은 Access-Token 응답 헤더로 내려간다. deviceState: NONE | MATCH | CHANGED */
public record AuthSessionResponse(String userId, @Nullable Integer loginType, @Nullable Integer userType, String deviceState) {

	static AuthSessionResponse from(LoginResult.SessionResult s) {
		return new AuthSessionResponse(s.userId(), s.loginType(), s.userType(), s.deviceState().name());
	}
}
