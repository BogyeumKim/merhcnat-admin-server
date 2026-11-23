package com.app.demo.auth.application;

import com.app.demo.auth.domain.ContactMasking;
import com.app.demo.auth.domain.OtpChannel;
import com.app.demo.user.domain.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/** 인증번호를 받을 수 있는 수단 하나. */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChannelOption {

	private final OtpChannel channel;

	/** 마스킹된 수신처 (예: 010-****-1234, te****@example.com). TOTP 는 null(응답에서 제외) */
	private final @Nullable String destination;

	static ChannelOption of(OtpChannel channel, User user) {
		String destination = channel.contactOf(user).map(contact -> ContactMasking.mask(channel, contact)).orElse(null);
		return new ChannelOption(channel, destination);
	}
}
