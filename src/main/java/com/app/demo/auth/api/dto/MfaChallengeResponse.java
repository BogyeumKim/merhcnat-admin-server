package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.ChannelOption;
import com.app.demo.auth.application.MfaChallenge;
import com.app.demo.auth.domain.OtpChannel;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * OTP 필요(200, code=MFA_REQUIRED) data.
 *
 * @param deviceState            NONE | MATCH | CHANGED
 * @param channels               사용 가능한 수단과 마스킹된 수신처. 연락처가 없으면 빈 배열
 * @param lastUsedChannel        마지막으로 인증에 성공한 수단. 없으면 응답에서 제외
 * @param challengeToken         이후 otp/send·otp/verify 에 쓰는 일회용 토큰
 * @param registeredUserIdMasked deviceState=CHANGED 일 때 이 기기에 현재 등록된 아이디(마스킹, 본인 또는 다른 계정)
 * @param deviceRegisteredToOtherAccount 이 기기가 다른 계정에 등록돼 있으면 true. 그 외 제외
 */
public record MfaChallengeResponse(
		String userId,
		int loginType,
		@Nullable Integer userType,
		String deviceState,
		List<Channel> channels,
		@Nullable String lastUsedChannel,
		String challengeToken,
		@Nullable String registeredUserIdMasked,
		@Nullable Boolean deviceRegisteredToOtherAccount) implements AuthStepResponse {

	public static MfaChallengeResponse from(MfaChallenge c) {
		OtpChannel last = c.lastUsedChannel();
		return new MfaChallengeResponse(c.userId(), c.loginType(), c.userType(), c.deviceState().name(),
				c.channels().stream().map(Channel::from).toList(), last == null ? null : last.name(),
				c.challengeToken(), c.registeredUserIdMasked(), c.deviceRegisteredToOtherAccount());
	}

	/**
	 * 인증 수단 하나.
	 *
	 * @param destination 마스킹된 수신처. TOTP 는 응답에서 제외
	 */
	public record Channel(String channel, @Nullable String destination) {

		static Channel from(ChannelOption option) {
			return new Channel(option.channel().name(), option.destination());
		}
	}
}
