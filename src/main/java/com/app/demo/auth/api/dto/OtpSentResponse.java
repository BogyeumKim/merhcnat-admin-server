package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.OtpSentResult;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/** OTP 발송 성공 data. remainingSends 는 이 챌린지에서 남은 발송 횟수. maskedPhone 은 전환 기간용(KAKAO/SMS 만, EMAIL 이면 응답에서 제외) */
@JsonPropertyOrder({"expiresInSeconds", "resendAfterSeconds", "remainingSends", "maskedDestination", "maskedPhone"})
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OtpSentResponse {

	private final long expiresInSeconds;

	private final long resendAfterSeconds;

	private final int remainingSends;

	private final String maskedDestination;

	private final @Nullable String maskedPhone;

	public static OtpSentResponse from(OtpSentResult r) {
		return OtpSentResponse.with()
				.expiresInSeconds(r.getExpiresInSeconds())
				.resendAfterSeconds(r.getResendAfterSeconds())
				.remainingSends(r.getRemainingSends())
				.maskedDestination(r.getMaskedDestination())
				.maskedPhone(r.getMaskedPhone())
				.create();
	}
}
