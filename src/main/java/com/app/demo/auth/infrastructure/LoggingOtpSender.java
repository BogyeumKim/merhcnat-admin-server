package com.app.demo.auth.infrastructure;

import com.app.demo.auth.domain.ContactMasking;
import com.app.demo.auth.domain.OtpChannel;
import com.app.demo.auth.domain.OtpSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발용: 실제 발송 대신 서버 콘솔 로그로 OTP 를 확인한다(수신처는 마스킹).
 * 운영에서는 알림톡/SMS 구현체와 Outlook 메일 발송 구현체로 교체한다.
 */
@Component
@Slf4j
class LoggingOtpSender implements OtpSender {

	@Override
	public void send(OtpChannel channel, String destination, String code) {
		log.warn("[DEV OTP] channel={} to={} code={}", channel, ContactMasking.mask(channel, destination), code);
	}
}
