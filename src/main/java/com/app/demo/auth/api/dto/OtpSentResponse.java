package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.OtpSentResult;
import org.jspecify.annotations.Nullable;

/** OTP 발송 성공 data. maskedPhone 은 전환 기간용(KAKAO/SMS 만, EMAIL 이면 응답에서 제외) */
public record OtpSentResponse(long expiresInSeconds, long resendAfterSeconds, String maskedDestination,
		@Nullable String maskedPhone) {

	public static OtpSentResponse from(OtpSentResult r) {
		return new OtpSentResponse(r.expiresInSeconds(), r.resendAfterSeconds(), r.maskedDestination(), r.maskedPhone());
	}
}
