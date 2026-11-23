package com.app.demo.auth.application;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** OTP 발송 요청 (비밀번호 대신 챌린지 토큰). */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OtpSendCommand {

	private final String challengeToken;

	private final String channel;
}
