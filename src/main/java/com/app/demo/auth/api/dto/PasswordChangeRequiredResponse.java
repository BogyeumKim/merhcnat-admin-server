package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.PasswordChangeChallenge;
import com.app.demo.user.domain.PasswordChangeReason;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 비밀번호 변경 필요(200, code=PASSWORD_CHANGE_REQUIRED) data. 아직 로그인 전이라 Access-Token 은 없다. */
@JsonPropertyOrder({"step", "message", "userId", "challengeToken", "reason"})
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PasswordChangeRequiredResponse implements AuthStepResponse {

	/** 임시·만료 비밀번호. challengeToken 으로 {@code /api/auth/password/temp} 를 호출한다 */
	@Override
	public String getStep() {
		return "PASSWORD_CHANGE_REQUIRED";
	}

	/** 앱이 분기에 쓰지 않는 참고 문구. 이유에 맞게 고른다 */
	private final String message;

	private final String userId;

	private final String challengeToken;

	/** TEMP_PASSWORD(임시비밀번호) | PASSWORD_EXPIRED(비밀번호 만료). 둘 다 해당하면 TEMP_PASSWORD */
	private final String reason;

	public static PasswordChangeRequiredResponse from(PasswordChangeChallenge c) {
		String reason = c.getReason().name();
		String message = messageOf(c.getReason());
		return PasswordChangeRequiredResponse.with()
				.message(message)
				.userId(c.getUserId())
				.challengeToken(c.getChallengeToken())
				.reason(reason)
				.create();
	}

	private static String messageOf(PasswordChangeReason reason) {
		return switch (reason) {
			case TEMP_PASSWORD -> "임시비밀번호입니다. 새 비밀번호를 등록해 주세요.";
			case PASSWORD_EXPIRED -> "비밀번호 사용 기간이 지났습니다. 새 비밀번호를 등록해 주세요.";
		};
	}
}
