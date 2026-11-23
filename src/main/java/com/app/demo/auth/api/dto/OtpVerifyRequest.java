package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.OtpVerifyCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/** OTP 검증 요청. channel: TOTP | KAKAO | SMS | EMAIL */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class OtpVerifyRequest {

	@NotBlank
	@Size(max = 2048)
	private final String challengeToken;

	@NotBlank
	@Size(max = 10)
	private final String channel;

	@NotBlank
	@Size(max = 10)
	private final String otp;

	public OtpVerifyCommand toCommand() {
		return OtpVerifyCommand.with()
				.challengeToken(challengeToken)
				.channel(channel)
				.otp(otp)
				.create();
	}
}
