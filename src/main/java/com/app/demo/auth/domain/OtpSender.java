package com.app.demo.auth.domain;

/** OTP 발송 포트. 운영에서는 알림톡/SMS/이메일 구현체로 교체한다. */
public interface OtpSender {

	/**
	 * OTP 평문을 사용자에게 발송한다.
	 *
	 * @param channel     KAKAO | SMS | EMAIL
	 * @param destination 수신처 원문 (휴대폰 번호 또는 이메일). 로그에 남길 때는 반드시 마스킹한다
	 * @param code        평문 OTP
	 */
	void send(OtpChannel channel, String destination, String code);
}
