package com.app.demo.auth.presentation;

import com.app.demo.auth.application.OtpSentResult;

/** OTP 발송 성공 data */
public record OtpSentResponse(long expiresInSeconds, long resendAfterSeconds, String maskedPhone) {

	static OtpSentResponse from(OtpSentResult r) {
		return new OtpSentResponse(r.expiresInSeconds(), r.resendAfterSeconds(), r.maskedPhone());
	}
}
