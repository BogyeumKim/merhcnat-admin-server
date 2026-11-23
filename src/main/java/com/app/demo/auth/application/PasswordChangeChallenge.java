package com.app.demo.auth.application;

import com.app.demo.user.domain.PasswordChangeReason;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 비밀번호 변경 안내: 변경이 필요한 이유와 이후 변경 API({@code /api/auth/password/temp})에 쓰는 일회용 챌린지 토큰. */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordChangeChallenge {

	private final String userId;

	private final String challengeToken;

	/** 임시비밀번호(TEMP_PASSWORD) / 비밀번호 만료(PASSWORD_EXPIRED) */
	private final PasswordChangeReason reason;
}
