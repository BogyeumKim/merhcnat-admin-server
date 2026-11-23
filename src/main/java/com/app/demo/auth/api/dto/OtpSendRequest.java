package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.OtpSendCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/** OTP 발송 요청 (loginType=1). 비밀번호 대신 1단계에서 받은 challengeToken 을 쓴다. channel: KAKAO | SMS | EMAIL */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class OtpSendRequest {

	@NotBlank
	@Size(max = 2048)
	private final String challengeToken;

	@NotBlank
	@Size(max = 10)
	private final String channel;

	public OtpSendCommand toCommand() {
		return OtpSendCommand.with()
				.challengeToken(challengeToken)
				.channel(channel)
				.create();
	}
}
