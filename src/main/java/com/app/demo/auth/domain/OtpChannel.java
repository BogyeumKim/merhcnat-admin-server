package com.app.demo.auth.domain;

import com.app.demo.user.domain.LoginType;
import com.app.demo.user.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** OTP 인증 수단. loginType 과 사용자에게 등록된 연락처에 따라 쓸 수 있는 수단이 정해진다. */
public enum OtpChannel {
	/** 인증앱(서버 발송 없음) — loginType=TOTP */
	TOTP,
	/** 카카오 알림톡 — loginType=MESSAGE, 휴대폰 번호 필요 */
	KAKAO,
	/** 문자 — loginType=MESSAGE, 휴대폰 번호 필요 */
	SMS,
	/** 이메일 — loginType=MESSAGE, 이메일 필요 */
	EMAIL;

	/** 이 사용자가 쓸 수 있는 수단 목록. MESSAGE 는 연락처가 있는 수단만 KAKAO, SMS, EMAIL 순서로 돌려준다. */
	public static List<OtpChannel> availableFor(LoginType type, User user) {
		return switch (type) {
			case TOTP -> List.of(TOTP);
			case MESSAGE -> Stream.of(KAKAO, SMS, EMAIL).filter(channel -> channel.contactOf(user).isPresent()).toList();
		};
	}

	/** 이 수단으로 인증번호를 받을 사용자 연락처(원문). TOTP 이거나 연락처가 없으면 empty */
	public Optional<String> contactOf(User user) {
		return switch (this) {
			case TOTP -> Optional.empty();
			case KAKAO, SMS -> user.hasPhone() ? Optional.ofNullable(user.phoneNumber()) : Optional.empty();
			case EMAIL -> user.hasEmail() ? Optional.ofNullable(user.email()) : Optional.empty();
		};
	}

	/** 휴대폰 번호로 받는 수단인지 */
	public boolean usesPhone() {
		return this == KAKAO || this == SMS;
	}

	/** 문자열(대소문자 무시) → 수단. 알 수 없으면 empty */
	public static Optional<OtpChannel> parse(String value) {
		return Stream.of(values()).filter(channel -> channel.name().equalsIgnoreCase(value)).findFirst();
	}
}
