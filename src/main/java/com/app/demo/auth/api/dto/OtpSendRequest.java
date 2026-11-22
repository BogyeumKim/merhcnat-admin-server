package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.OtpSendCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** OTP 발송 요청 (loginType=1). 비밀번호 대신 1단계에서 받은 challengeToken 을 쓴다. channel: KAKAO | SMS | EMAIL */
public record OtpSendRequest(@NotBlank @Size(max = 2048) String challengeToken, @NotBlank @Size(max = 10) String channel) {

	public OtpSendCommand toCommand() {
		return new OtpSendCommand(challengeToken, channel);
	}
}
