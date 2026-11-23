package com.app.demo.auth.api.dto;

import com.app.demo.auth.application.ChannelOption;
import com.app.demo.auth.application.MfaChallenge;
import com.app.demo.auth.domain.OtpChannel;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/** OTP 필요(200, code=MFA_REQUIRED) data. */
@JsonPropertyOrder({"step", "message", "userId", "loginType", "userType", "deviceState", "channels", "lastUsedChannel", "challengeToken", "registeredUserIdMasked", "deviceRegisteredToOtherAccount"})
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class MfaChallengeResponse implements AuthStepResponse {

	/** id/pw 통과, OTP 필요. challengeToken 으로 otp/send·otp/verify 를 진행한다 */
	@Override
	public String getStep() {
		return "MFA_REQUIRED";
	}

	/** 앱이 분기에 쓰지 않는 참고 문구 */
	public String getMessage() {
		return "추가 인증이 필요합니다.";
	}

	private final String userId;

	private final int loginType;

	private final @Nullable Integer userType;

	/** NONE | MATCH | CHANGED */
	private final String deviceState;

	/** 사용 가능한 수단과 마스킹된 수신처. 연락처가 없으면 빈 배열 */
	private final List<Channel> channels;

	/** 마지막으로 인증에 성공한 수단. 없으면 응답에서 제외 */
	private final @Nullable String lastUsedChannel;

	/** 이후 otp/send·otp/verify 에 쓰는 일회용 토큰 */
	private final String challengeToken;

	/** deviceState=CHANGED 일 때 이 기기에 현재 등록된 아이디(마스킹, 본인 또는 다른 계정) */
	private final @Nullable String registeredUserIdMasked;

	/** 이 기기가 다른 계정에 등록돼 있으면 true. 그 외 제외 */
	private final @Nullable Boolean deviceRegisteredToOtherAccount;

	public static MfaChallengeResponse from(MfaChallenge c) {
		OtpChannel last = c.getLastUsedChannel();
		String lastUsedChannel = last == null ? null : last.name();
		String deviceState = c.getDeviceState().name();
		List<Channel> channels = c.getChannels().stream().map(Channel::from).toList();
		return MfaChallengeResponse.with()
				.userId(c.getUserId())
				.loginType(c.getLoginType())
				.userType(c.getUserType())
				.deviceState(deviceState)
				.channels(channels)
				.lastUsedChannel(lastUsedChannel)
				.challengeToken(c.getChallengeToken())
				.registeredUserIdMasked(c.getRegisteredUserIdMasked())
				.deviceRegisteredToOtherAccount(c.getDeviceRegisteredToOtherAccount())
				.create();
	}

	/** 인증 수단 하나. */
	@JsonPropertyOrder({"channel", "destination"})
	@Getter
	@Builder(builderMethodName = "with", buildMethodName = "create")
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class Channel {

		private final String channel;

		/** 마스킹된 수신처. TOTP 는 응답에서 제외 */
		private final @Nullable String destination;

		static Channel from(ChannelOption option) {
			String channel = option.getChannel().name();
			return Channel.with()
					.channel(channel)
					.destination(option.getDestination())
					.create();
		}
	}
}
