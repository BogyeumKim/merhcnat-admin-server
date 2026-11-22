package com.app.demo.auth.infrastructure;

import com.app.demo.auth.domain.OtpChannel;
import com.app.demo.auth.domain.OtpSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 로컬 개발용: 실제 발송 대신 서버 콘솔 로그로 OTP 를 확인한다. (운영 금지 — SMS/알림톡 구현체로 교체) */
@Component
@Slf4j
class LoggingOtpSender implements OtpSender {

	@Override
	public void send(OtpChannel channel, String phoneNumber, String code) {
		log.warn("[DEV OTP] channel={} phone={} code={}", channel, phoneNumber, code);
	}
}
