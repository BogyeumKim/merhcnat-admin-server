package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.OtpSentResult;
import org.jspecify.annotations.Nullable;

/** OTP 발송 성공 data. remainingSends 는 이 챌린지에서 남은 발송 횟수. maskedPhone 은 전환 기간용(KAKAO/SMS 만, EMAIL 이면 응답에서 제외) */
public record OtpSentResponse(long expiresInSeconds, long resendAfterSeconds, int remainingSends, String maskedDestination,
		@Nullable String maskedPhone) {

	public static OtpSentResponse from(OtpSentResult r) {
		return new OtpSentResponse(r.expiresInSeconds(), r.resendAfterSeconds(), r.remainingSends(), r.maskedDestination(), r.maskedPhone());
	}
}
