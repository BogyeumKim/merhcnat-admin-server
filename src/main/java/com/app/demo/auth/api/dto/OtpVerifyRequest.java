package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.OtpVerifyCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** OTP 검증 요청. channel: TOTP | KAKAO | SMS | EMAIL */
public record OtpVerifyRequest(
		@NotBlank @Size(max = 2048) String challengeToken,
		@NotBlank @Size(max = 10) String channel,
		@NotBlank @Size(max = 10) String otp) {

	public OtpVerifyCommand toCommand() {
		return new OtpVerifyCommand(challengeToken, channel, otp);
	}
}
