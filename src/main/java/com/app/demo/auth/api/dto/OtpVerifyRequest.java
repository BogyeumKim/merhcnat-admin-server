package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.OtpVerifyCommand;
import jakarta.validation.constraints.NotBlank;

/** OTP 검증 요청. channel: TOTP | KAKAO | SMS */
public record OtpVerifyRequest(@NotBlank String challengeToken, @NotBlank String channel, @NotBlank String otp) {

	public OtpVerifyCommand toCommand() {
		return new OtpVerifyCommand(challengeToken, channel, otp);
	}
}
