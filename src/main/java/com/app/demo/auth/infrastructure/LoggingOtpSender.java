package com.app.demo.auth.infrastructure;

import com.app.demo.auth.domain.OtpChannel;
import com.app.demo.auth.domain.OtpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 로컬 개발용: 실제 발송 대신 서버 콘솔 로그로 OTP 를 확인한다. (운영 금지 — SMS/알림톡 구현체로 교체) */
@Component
class LoggingOtpSender implements OtpSender {

	private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

	@Override
	public void send(OtpChannel channel, String phoneNumber, String code) {
		log.warn("[DEV OTP] channel={} phone={} code={}", channel, phoneNumber, code);
	}
}
