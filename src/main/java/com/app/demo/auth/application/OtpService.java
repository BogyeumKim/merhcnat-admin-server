package com.app.demo.auth.application;

import com.app.demo.auth.config.OtpProperties;
import com.app.demo.auth.domain.ContactMasking;
import com.app.demo.auth.domain.OtpChannel;
import com.app.demo.auth.domain.OtpHasher;
import com.app.demo.auth.domain.OtpSender;
import com.app.demo.auth.domain.TotpVerifier;
import com.app.demo.auth.exception.AuthErrorCode;
import com.app.demo.global.api.ErrorData;
import com.app.demo.global.exception.BusinessException;
import com.app.demo.user.domain.LoginType;
import com.app.demo.user.domain.PasswordChangeReason;
import com.app.demo.user.domain.User;
import com.app.demo.user.domain.UserInfo;
import com.app.demo.user.domain.UserInfoRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.OptionalLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 로그인 2단계: OTP 발송(카카오/SMS/이메일)과 검증(TOTP/카카오/SMS/이메일). 비밀번호 대신 1단계의 챌린지 토큰으로 진행한다.
 * 검증에 성공하면 기기를 등록하고 Access-Token 을 발급한다(임시비밀번호 사용자는 비밀번호 변경 안내).
 * OTP 오류는 id/pw 오류와 같은 횟수에 합산되며, 실패 횟수가 롤백되면 안 되므로 의도적으로 {@code @Transactional} 을 쓰지 않는다.
 */
@Service
@RequiredArgsConstructor
public class OtpService {

	private static final SecureRandom RANDOM = new SecureRandom();
	/** OTP 만료 후에도 챌린지를 이만큼 더 유지해 입력 도중 만료되지 않게 한다 */
	private static final long CHALLENGE_GRACE_SECONDS = 60;

	private final UserInfoRepository infos;
	private final TotpVerifier totp;
	private final OtpSender sender;
	private final OtpHasher hasher;
	private final OtpProperties otpProps;
	private final AccountLockService accountLock;
	private final ChallengeService challenges;
	private final LoginCompletion completion;
	private final PasswordChangePolicy passwordChangePolicy;
	private final Clock clock;

	/**
	 * 카카오/SMS/이메일로 새 OTP 를 보낸다. 사용자당 유효 OTP 는 1개라 이전 코드(다른 수단으로 보낸 것 포함)는 자동 폐기된다.
	 * 같은 수단 재발송만 간격 제한을 두고, 수단을 바꾸면 즉시 보낸다. 남용 방지를 위해 챌린지당·시간당 발송 횟수 상한을 둔다.
	 */
	public OtpSentResult sendOtp(OtpSendCommand cmd) {
		Challenge ch = challenges.loadMfa(cmd.challengeToken());
		OtpChannel channel = requireChannel(ch.user(), cmd.channel());
		if (channel == OtpChannel.TOTP) {
			throw new BusinessException(AuthErrorCode.OTP_CHANNEL_NOT_SENDABLE);
		}
		String destination = channel.contactOf(ch.user())
				.orElseThrow(() -> new BusinessException(AuthErrorCode.OTP_CHANNEL_INVALID));
		LocalDateTime now = LocalDateTime.now(clock);
		reserveSend(ch, channel, now);

		String code = generateCode();
		// 평문은 저장하지 않고 해시만 저장
		infos.saveOtp(ch.user().userId(), hasher.hash(ch.user().userId(), code), channel.name(),
				now.plusSeconds(otpProps.ttlSeconds()), now);
		sender.send(channel, destination, code);
		return sentResult(channel, ContactMasking.mask(channel, destination));
	}

	/** OTP 검증 → 챌린지 소진 → 로그인 완료. 임시비밀번호·만료된 비밀번호 사용자는 비밀번호 변경 안내(PASSWORD_CHANGE_REQUIRED) */
	public LoginStep verifyOtp(OtpVerifyCommand cmd) {
		Challenge ch = challenges.loadMfa(cmd.challengeToken());
		User user = ch.user();
		OtpChannel channel = requireChannel(user, cmd.channel());
		if (channel == OtpChannel.TOTP) {
			verifyTotp(ch, cmd.otp());
		} else {
			verifyMessageOtp(ch, channel, cmd.otp());
		}
		challenges.consume(ch);
		infos.updateLastOtpChannel(user.userId(), channel.name());

		// 임시비밀번호·만료된 비밀번호: 새 비밀번호를 등록하기 전에는 로그인을 완료(Access-Token 발급/기기 등록)하지 않는다.
		Optional<PasswordChangeReason> changeReason = passwordChangePolicy.reasonFor(user);
		if (changeReason.isPresent()) {
			return new LoginStep.PasswordChangeRequired(
					challenges.issuePasswordChange(user.userId(), ch.claims().device(), changeReason.get()));
		}
		return completion.completeVerified(user, ch.claims().device());
	}

	/** TOTP: 저장된 코드 없이 user.totp_secret 으로 현재 시각 기준 계산해 비교. 같은(또는 이전) time-step 재사용도 거부 */
	private void verifyTotp(Challenge ch, String otp) {
		if (isDevFixedCode(otp)) {
			return; // [개발용] 고정 OTP 와 일치하면 TOTP 계산/등록 여부와 무관하게 통과
		}
		User user = ch.user();
		String secret = user.totpSecret();
		if (secret == null || secret.isBlank()) {
			throw new BusinessException(AuthErrorCode.TOTP_NOT_ENROLLED);
		}
		long step = accountLock.verifySecret(user.userId(), () -> unusedTotpStep(ch, secret, otp), OtpService::otpInvalid);
		infos.updateLastTotpStep(user.userId(), step);
	}

	/** 코드가 맞고 아직 쓰지 않은 time-step 이면 그 step, 아니면 empty (같은·이전 step 재사용 거부) */
	private Optional<Long> unusedTotpStep(Challenge ch, String secret, String otp) {
		OptionalLong step = totp.verify(secret, otp);
		return step.isPresent() && !ch.info().isTotpReplay(step.getAsLong()) ? Optional.of(step.getAsLong()) : Optional.empty();
	}

	/** 카카오/SMS/이메일: user_info 에 저장된 해시와 비교 후 1회용으로 소진 */
	private void verifyMessageOtp(Challenge ch, OtpChannel channel, String otp) {
		String userId = ch.user().userId();
		String stored = ch.info().otpCodeHash();
		if (stored == null || !ch.info().hasValidOtp(channel.name(), LocalDateTime.now(clock))) {
			throw new BusinessException(AuthErrorCode.OTP_EXPIRED);
		}
		accountLock.verifySecret(userId, () -> hasher.matches(userId, otp, stored), OtpService::otpInvalid);
		// 해시가 그대로일 때만 비워서, 동시 요청으로 같은 코드를 두 번 쓰지 못하게 한다(늦은 요청은 이미 쓴 코드로 본다).
		if (!infos.consumeOtp(userId, stored)) {
			throw new BusinessException(AuthErrorCode.OTP_EXPIRED);
		}
	}

	/** loginType 과 등록된 연락처 기준으로 이 사용자가 쓸 수 있는 수단인지 확인하고 enum 으로 변환 */
	private static OtpChannel requireChannel(User user, String raw) {
		LoginType type = user.loginType().orElseThrow(() -> new BusinessException(AuthErrorCode.UNSUPPORTED_LOGIN_TYPE));
		return OtpChannel.parse(raw)
				.filter(OtpChannel.availableFor(type, user)::contains)
				.orElseThrow(() -> new BusinessException(AuthErrorCode.OTP_CHANNEL_INVALID));
	}

	/** [전환 기간] 휴대폰 수단이면 maskedPhone 도 같은 값으로 내려준다. */
	private OtpSentResult sentResult(OtpChannel channel, String maskedDestination) {
		String maskedPhone = channel.usesPhone() ? maskedDestination : null;
		return new OtpSentResult(otpProps.ttlSeconds(), otpProps.resendSeconds(), maskedDestination, maskedPhone);
	}

	/**
	 * 발송 1회를 원자적으로 예약한다(같은 수단 재발송 간격 + 챌린지당·시간당 상한 + 챌린지 일치를 UPDATE 한 문장으로 확인).
	 * 동시에 같은 수단으로 여러 번 요청해도 1건만 발송된다. 예약에 실패하면 어떤 규칙에 걸렸는지 다시 읽어 알려 준다.
	 * 예약과 함께 챌린지 유효 시각을 연장한다.
	 */
	private void reserveSend(Challenge ch, OtpChannel channel, LocalDateTime now) {
		String userId = ch.user().userId();
		boolean reserved = infos.reserveOtpSend(userId, ch.claims().jti(), channel.name(),
				now.minusSeconds(otpProps.resendSeconds()), otpProps.maxSendsPerChallenge(), otpProps.maxSendsPerHour(),
				extendedChallengeExpiry(ch, now), now);
		if (!reserved) {
			throw sendRejection(infos.findOrCreate(userId), channel, now);
		}
	}

	/**
	 * 새 OTP 를 입력할 시간을 보장하도록 (OTP 만료 + 60초)까지 연장한다. 지금 남은 시간보다 줄이지는 않고,
	 * 토큰의 절대 상한(로그인 + 15분)은 넘지 않는다.
	 */
	private LocalDateTime extendedChallengeExpiry(Challenge ch, LocalDateTime now) {
		LocalDateTime wanted = now.plusSeconds(otpProps.ttlSeconds() + CHALLENGE_GRACE_SECONDS);
		LocalDateTime current = ch.info().mfaExpiresAt();
		if (current != null && current.isAfter(wanted)) {
			wanted = current;
		}
		LocalDateTime cap = ch.claims().expiresAt();
		return wanted.isBefore(cap) ? wanted : cap;
	}

	/** 예약 실패 사유: 같은 수단 재발송 간격(429) → 챌린지당 상한(429) → 시간당 상한(429) → 그 외는 챌린지가 바뀐 경우(401) */
	private BusinessException sendRejection(UserInfo info, OtpChannel channel, LocalDateTime now) {
		long resendWait = info.resendWaitSeconds(channel.name(), now, otpProps.resendSeconds());
		if (resendWait > 0) {
			return new BusinessException(AuthErrorCode.OTP_RESEND_TOO_SOON, ErrorData.retryAfter(resendWait));
		}
		if (info.hasReachedChallengeSendLimit(otpProps.maxSendsPerChallenge())) {
			return new BusinessException(AuthErrorCode.OTP_SEND_LIMIT_EXCEEDED);
		}
		long hourlyWait = info.hourlySendLimitWaitSeconds(now, otpProps.maxSendsPerHour());
		if (hourlyWait > 0) {
			return new BusinessException(AuthErrorCode.OTP_HOURLY_SEND_LIMIT_EXCEEDED, ErrorData.retryAfter(hourlyWait));
		}
		return new BusinessException(AuthErrorCode.CHALLENGE_EXPIRED);
	}

	/** OTP 오류: 남은 시도 횟수를 data.remainingAttempts 로 알려준다 */
	private static BusinessException otpInvalid(int remaining) {
		return new BusinessException(AuthErrorCode.OTP_INVALID, ErrorData.remaining(remaining));
	}

	/** 암호학적으로 안전한 난수로 OTP 숫자열 생성. [개발용] 고정 OTP 가 설정되어 있으면 그 값을 쓴다. */
	private String generateCode() {
		String fixed = otpProps.devFixedCode();
		if (fixed != null && !fixed.isBlank()) {
			return fixed;
		}
		StringBuilder sb = new StringBuilder(otpProps.length());
		for (int i = 0; i < otpProps.length(); i++) {
			sb.append(RANDOM.nextInt(10));
		}
		return sb.toString();
	}

	/** [개발용] 입력값이 설정된 고정 OTP 와 같은지 (설정이 없으면 항상 false) */
	private boolean isDevFixedCode(String otp) {
		String fixed = otpProps.devFixedCode();
		return fixed != null && !fixed.isBlank() && fixed.equals(otp);
	}
}
