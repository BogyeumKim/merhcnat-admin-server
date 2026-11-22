package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.OtpSentResult;

/** OTP 발송 성공 data */
public record OtpSentResponse(long expiresInSeconds, long resendAfterSeconds, String maskedPhone) {

	public static OtpSentResponse from(OtpSentResult r) {
		return new OtpSentResponse(r.expiresInSeconds(), r.resendAfterSeconds(), r.maskedPhone());
	}
}
