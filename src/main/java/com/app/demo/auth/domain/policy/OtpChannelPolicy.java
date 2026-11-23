package com.app.demo.auth.domain.policy;

import com.app.demo.auth.domain.OtpChannel;
import com.app.demo.auth.exception.AuthErrorCode;
import com.app.demo.global.exception.ErrorCode;
import com.app.demo.user.domain.LoginType;
import com.app.demo.user.domain.User;
import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/** 이 사용자가 고른 OTP 수단을 쓸 수 있는지 가른다. 로그인 유형 → 등록된 연락처 기준 수단 → (발송이면) 발송 가능 → 수신처 순. */
@UtilityClass
public class OtpChannelPolicy {

	/** ex) 지원하지 않는 loginType → {@code UNSUPPORTED_LOGIN_TYPE}, 연락처 없는 SMS·알 수 없는 문자열 → {@code OTP_CHANNEL_INVALID} */
	public Optional<ErrorCode> checkChannel(User user, String rawChannel) {
		Optional<LoginType> type = user.loginType();
		if (type.isEmpty()) {
			return Optional.of(AuthErrorCode.UNSUPPORTED_LOGIN_TYPE);
		}
		List<OtpChannel> available = OtpChannel.availableFor(type.orElseThrow(), user);
		Optional<OtpChannel> channel = OtpChannel.parse(rawChannel);
		if (channel.isEmpty() || !available.contains(channel.orElseThrow())) {
			return Optional.of(AuthErrorCode.OTP_CHANNEL_INVALID);
		}
		return Optional.empty();
	}

	/** ex) TOTP → {@code OTP_CHANNEL_NOT_SENDABLE}, 수신처가 없는 수단 → {@code OTP_CHANNEL_INVALID} */
	public Optional<ErrorCode> checkSendable(User user, String rawChannel) {
		Optional<ErrorCode> channelRejection = checkChannel(user, rawChannel);
		if (channelRejection.isPresent()) {
			return channelRejection;
		}
		OtpChannel channel = OtpChannel.parse(rawChannel).orElseThrow();
		if (channel == OtpChannel.TOTP) {
			return Optional.of(AuthErrorCode.OTP_CHANNEL_NOT_SENDABLE);
		}
		if (channel.contactOf(user).isEmpty()) {
			return Optional.of(AuthErrorCode.OTP_CHANNEL_INVALID);
		}
		return Optional.empty();
	}
}
