package com.app.demo.auth.application;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** OTP 검증 요청. */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OtpVerifyCommand {

	private final String challengeToken;

	private final String channel;

	private final String otp;
}
