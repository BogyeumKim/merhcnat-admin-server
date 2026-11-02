package com.app.demo.auth.domain;

/** OTP 발송 포트. 운영에서는 SMS/알림톡 구현체로 교체한다. */
public interface OtpSender {

	/**
	 * @param channel     KAKAO | SMS
	 * @param phoneNumber 수신 번호
	 * @param code        평문 OTP
	 */
	void send(OtpChannel channel, String phoneNumber, String code);
}
