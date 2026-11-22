package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.OtpSendCommand;
import jakarta.validation.constraints.NotBlank;

/** OTP 발송 요청 (loginType=1). 비밀번호 대신 1단계에서 받은 challengeToken 을 쓴다. channel: KAKAO | SMS */
public record OtpSendRequest(@NotBlank String challengeToken, @NotBlank String channel) {

	public OtpSendCommand toCommand() {
		return new OtpSendCommand(challengeToken, channel);
	}
}
