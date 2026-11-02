package com.app.demo.auth.domain;

import com.app.demo.user.domain.LoginType;
import java.util.List;
import java.util.Optional;

/** OTP 인증 수단. loginType 별로 허용되는 수단이 다르다. */
public enum OtpChannel {
	/** 인증앱(서버 발송 없음) — loginType=TOTP */
	TOTP,
	/** 카카오 — loginType=MESSAGE */
	KAKAO,
	/** 문자 — loginType=MESSAGE */
	SMS;

	/** 해당 loginType 에서 사용 가능한 수단 목록 */
	public static List<OtpChannel> forLoginType(LoginType type) {
		return switch (type) {
			case TOTP -> List.of(TOTP);
			case MESSAGE -> List.of(KAKAO, SMS);
		};
	}

	/** 문자열(대소문자 무시) → 수단. 알 수 없으면 empty */
	public static Optional<OtpChannel> parse(String value) {
		for (OtpChannel c : values()) {
			if (c.name().equalsIgnoreCase(value)) {
				return Optional.of(c);
			}
		}
		return Optional.empty();
	}
}
