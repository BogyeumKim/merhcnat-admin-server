package com.app.demo.auth.application;

import com.app.demo.auth.config.OtpProperties;
import com.app.demo.auth.domain.OtpChannel;
import com.app.demo.auth.domain.OtpHasher;
import com.app.demo.auth.domain.OtpSender;
import com.app.demo.auth.domain.TotpVerifier;
import com.app.demo.auth.exception.AuthErrorCode;
import com.app.demo.global.api.ErrorData;
import com.app.demo.global.exception.BusinessException;
import com.app.demo.user.domain.LoginType;
import com.app.demo.user.domain.User;
import com.app.demo.user.domain.UserInfo;
import com.app.demo.user.domain.UserInfoRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.OptionalLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 로그인 2단계: OTP 발송(카카오/SMS)과 검증(TOTP/카카오/SMS). 비밀번호 대신 1단계의 챌린지 토큰으로 진행한다.
 * 검증에 성공하면 기기를 등록하고 Access-Token 을 발급한다(임시비밀번호 사용자는 403 PASSWORD_CHANGE_REQUIRED).
 * OTP 오류는 id/pw 오류와 같은 횟수에 합산되며, 실패 횟수가 롤백되면 안 되므로 의도적으로 {@code @Transactional} 을 쓰지 않는다.
 */
@Service
@RequiredArgsConstructor
public class OtpService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final UserInfoRepository infos;
	private final TotpVerifier totp;
	private final OtpSender sender;
	private final OtpHasher hasher;
	private final OtpProperties otpProps;
	private final AccountLockService accountLock;
	private final ChallengeService challenges;
	private final LoginCompletion completion;
	private final Clock clock;

	/** 카카오/SMS 로 새 OTP 를 보낸다. 사용자당 유효 OTP 는 1개라 이전 코드는 자동 폐기된다. */
	public OtpSentResult sendOtp(OtpSendCommand cmd) {
		Challenge ch = challenges.loadMfa(cmd.challengeToken());
		OtpChannel channel = requireChannel(ch.user(), cmd.channel());
		if (channel == OtpChannel.TOTP) {
			throw new BusinessException(AuthErrorCode.OTP_CHANNEL_NOT_SENDABLE);
		}
		String phone = requirePhone(ch.user());
		LocalDateTime now = LocalDateTime.now(clock);
		checkResendInterval(ch.info(), now);

		String code = generateCode();
		// 평문은 저장하지 않고 해시만 저장
		infos.saveOtp(ch.user().userId(), hasher.hash(ch.user().userId(), code), channel.name(),
				now.plusSeconds(otpProps.ttlSeconds()), now);
		sender.send(channel, phone, code);
		return new OtpSentResult(otpProps.ttlSeconds(), otpProps.resendSeconds(), maskPhone(phone));
	}

	/** OTP 검증 → 챌린지 소진 → 로그인 완료. 임시비밀번호 사용자는 403 PASSWORD_CHANGE_REQUIRED */
	public LoginSuccess verifyOtp(OtpVerifyCommand cmd) {
		Challenge ch = challenges.loadMfa(cmd.challengeToken());
		User user = ch.user();
		OtpChannel channel = requireChannel(user, cmd.channel());
		if (channel == OtpChannel.TOTP) {
			verifyTotp(ch, cmd.otp());
		} else {
			verifyMessageOtp(ch, channel, cmd.otp());
		}
		challenges.consume(ch);

		// 임시비밀번호: 새 비밀번호를 등록하기 전에는 로그인을 완료(Access-Token 발급/기기 등록)하지 않는다.
		if (user.tempPassword()) {
			PasswordChangeChallenge challenge = challenges.issuePasswordChange(user.userId(), ch.claims().device());
			throw new BusinessException(AuthErrorCode.PASSWORD_CHANGE_REQUIRED, challenge);
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
		OptionalLong step = totp.verify(secret, otp);
		if (step.isEmpty() || ch.info().isTotpReplay(step.getAsLong())) {
			throw otpFail(user.userId());
		}
		infos.updateLastTotpStep(user.userId(), step.getAsLong());
	}

	/** 카카오/SMS: user_info 에 저장된 해시와 비교 후 1회용으로 소진 */
	private void verifyMessageOtp(Challenge ch, OtpChannel channel, String otp) {
		String userId = ch.user().userId();
		String stored = ch.info().otpCodeHash();
		if (stored == null || !ch.info().hasValidOtp(channel.name(), LocalDateTime.now(clock))) {
			throw new BusinessException(AuthErrorCode.OTP_EXPIRED);
		}
		if (!hasher.matches(userId, otp, stored)) {
			throw otpFail(userId);
		}
		// 해시가 그대로일 때만 비워서, 동시 요청으로 같은 코드를 두 번 쓰지 못하게 한다.
		if (!infos.consumeOtp(userId, stored)) {
			throw otpFail(userId);
		}
	}

	/** loginType 에서 허용되는 수단인지 확인하고 enum 으로 변환 */
	private static OtpChannel requireChannel(User user, String raw) {
		LoginType type = user.loginType().orElseThrow(() -> new BusinessException(AuthErrorCode.UNSUPPORTED_LOGIN_TYPE));
		return OtpChannel.parse(raw)
				.filter(OtpChannel.forLoginType(type)::contains)
				.orElseThrow(() -> new BusinessException(AuthErrorCode.OTP_CHANNEL_INVALID));
	}

	private static String requirePhone(User user) {
		String phone = user.phoneNumber();
		if (phone == null || phone.isBlank()) {
			throw new BusinessException(AuthErrorCode.PHONE_NOT_REGISTERED);
		}
		return phone;
	}

	/** 재발송 간격 제한 (문자 폭탄/비용 방지) */
	private void checkResendInterval(UserInfo info, LocalDateTime now) {
		long wait = info.resendWaitSeconds(now, otpProps.resendSeconds());
		if (wait > 0) {
			throw new BusinessException(AuthErrorCode.OTP_RESEND_TOO_SOON, ErrorData.retryAfter(wait));
		}
	}

	/** OTP 오류: 남은 시도 횟수를 data.remainingAttempts 로 알려준다 */
	private BusinessException otpFail(String userId) {
		return accountLock.recordFailure(userId,
				remaining -> new BusinessException(AuthErrorCode.OTP_INVALID, ErrorData.remaining(remaining)));
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

	/** 전화번호 뒤 4자리만 노출 */
	private static String maskPhone(String phone) {
		return phone.length() <= 4 ? "****" : "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
	}
}
