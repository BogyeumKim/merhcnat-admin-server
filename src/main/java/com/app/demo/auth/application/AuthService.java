package com.app.demo.auth.application;

import com.app.demo.auth.config.AuthProperties;
import com.app.demo.auth.config.OtpProperties;
import com.app.demo.auth.domain.MfaClaims;
import com.app.demo.auth.domain.OtpChannel;
import com.app.demo.auth.domain.OtpHasher;
import com.app.demo.auth.domain.OtpSender;
import com.app.demo.auth.domain.TokenProvider;
import com.app.demo.auth.domain.TotpVerifier;
import com.app.demo.global.api.ErrorData;
import com.app.demo.global.exception.BusinessException;
import com.app.demo.user.domain.DeviceInfo;
import com.app.demo.user.domain.DeviceState;
import com.app.demo.user.domain.LoginType;
import com.app.demo.user.domain.User;
import com.app.demo.user.domain.UserInfo;
import com.app.demo.user.domain.UserInfoRepository;
import com.app.demo.user.domain.UserRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 로그인(2단계 인증) 유스케이스.
 *
 * <ol>
 *   <li>{@link #login}      : id/pw + 기기정보 검증. 등록 기기 + loginType=MESSAGE 면 바로 완료, 아니면 MFA 챌린지 토큰 발급</li>
 *   <li>{@link #sendOtp}    : (loginType=MESSAGE) 카카오/SMS OTP 발송</li>
 *   <li>{@link #verifyOtp}  : OTP 검증 → 기기 등록 + Access-Token 발급</li>
 * </ol>
 *
 * <p>잠금 정책: id/pw 오류와 OTP 오류를 합쳐 오류 횟수를 +1 하고, 한도에 도달하면 이후 모든 단계에서 423 으로 차단한다
 * (관리자 해제). 횟수는 "최종 로그인 성공" 때만 0 으로 초기화한다.
 *
 * <p>주의: 의도적으로 {@code @Transactional} 을 쓰지 않는다. 실패 횟수 증가는 예외가 던져져도 반드시 커밋돼야 하기 때문이다.
 */
@Service
public class AuthService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final UserRepository users;
	private final UserInfoRepository infos;
	private final TokenProvider tokens;
	private final TotpVerifier totp;
	private final OtpSender sender;
	private final OtpHasher hasher;
	private final AuthProperties authProps;
	private final OtpProperties otpProps;

	public AuthService(UserRepository users, UserInfoRepository infos, TokenProvider tokens, TotpVerifier totp,
			OtpSender sender, OtpHasher hasher, AuthProperties authProps, OtpProperties otpProps) {
		this.users = users;
		this.infos = infos;
		this.tokens = tokens;
		this.totp = totp;
		this.sender = sender;
		this.hasher = hasher;
		this.authProps = authProps;
		this.otpProps = otpProps;
	}

	// =====================================================================
	// 1단계: id/pw 검증 (+ 기기 판별). 비밀번호는 여기서만 받는다.
	// =====================================================================
	public LoginResult login(LoginCommand cmd) {
		DeviceInfo device = validateDevice(cmd.device());
		User user = authenticate(cmd.userId(), cmd.password());
		LoginType type = user.loginType().orElseThrow(AuthService::unsupported);
		DeviceState state = deviceState(user.userId(), device);

		// 이미 등록된 기기(MATCH) + loginType=MESSAGE → OTP 생략하고 바로 로그인 완료.
		// loginType=TOTP 는 기기와 관계없이 항상 OTP 를 요구한다.
		if (type == LoginType.MESSAGE && state == DeviceState.MATCH) {
			String accessToken = finishLogin(user, device);
			return new LoginResult.Completed(
					new LoginResult.SessionResult(user.userId(), type.code(), user.userType(), state), accessToken);
		}
		// 그 외: OTP 가 필요하므로 일회용 챌린지 토큰을 내려준다.
		String challengeToken = issueChallenge(user.userId(), device);
		return new LoginResult.MfaRequired(new LoginResult.ChallengeResult(
				user.userId(), type.code(), user.userType(), state, OtpChannel.forLoginType(type), challengeToken));
	}

	// =====================================================================
	// loginType=MESSAGE: SMS/카카오 OTP 발송
	// =====================================================================
	public OtpSentResult sendOtp(OtpSendCommand cmd) {
		Challenge ch = loadChallenge(cmd.challengeToken());
		OtpChannel channel = requireChannel(ch.user(), cmd.channel());
		if (channel == OtpChannel.TOTP) {
			throw bad("OTP_CHANNEL_INVALID", "TOTP는 발송이 필요 없습니다.");
		}
		String phone = ch.user().phoneNumber();
		if (phone == null || phone.isBlank()) {
			throw new BusinessException(HttpStatus.CONFLICT, "PHONE_NOT_REGISTERED", "등록된 연락처가 없습니다.");
		}

		// 재발송 간격 제한 (문자 폭탄/비용 방지)
		LocalDateTime now = LocalDateTime.now();
		long wait = ch.info().resendWaitSeconds(now, otpProps.resendSeconds());
		if (wait > 0) {
			throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "OTP_RESEND_TOO_SOON",
					"잠시 후 다시 요청해 주세요.", ErrorData.retryAfter(wait));
		}
		String code = generateCode();
		// 평문은 저장하지 않고 해시만 저장. 사용자당 유효 OTP 1개라 이전 코드는 자동 폐기된다.
		infos.saveOtp(ch.user().userId(), hasher.hash(ch.user().userId(), code), channel.name(),
				now.plusSeconds(otpProps.ttlSeconds()), now);
		sender.send(channel, phone, code);
		return new OtpSentResult(otpProps.ttlSeconds(), otpProps.resendSeconds(), mask(phone));
	}

	// =====================================================================
	// OTP 검증 → 성공 시 기기 등록 + Access-Token 발급
	// =====================================================================
	public VerifyResult verifyOtp(OtpVerifyCommand cmd) {
		Challenge ch = loadChallenge(cmd.challengeToken());
		User user = ch.user();
		OtpChannel channel = requireChannel(user, cmd.channel());

		if (channel == OtpChannel.TOTP) {
			// [개발용] 고정 OTP 가 설정되어 있고 일치하면 TOTP 계산/등록 여부와 무관하게 통과
			if (!isDevFixedCode(cmd.otp())) {
				// TOTP: 저장된 코드 없이 user.totp_secret 으로 현재 시각 기준 계산해 비교
				String secret = user.totpSecret();
				if (secret == null || secret.isBlank()) {
					throw new BusinessException(HttpStatus.CONFLICT, "TOTP_NOT_ENROLLED", "TOTP가 등록되지 않았습니다.");
				}
				OptionalLong step = totp.verify(secret, cmd.otp());
				// 같은(또는 이전) time-step 의 코드를 다시 쓰는 재사용(replay)도 거부
				if (step.isEmpty() || ch.info().isTotpReplay(step.getAsLong())) {
					throw otpFail(user.userId());
				}
				infos.updateLastTotpStep(user.userId(), step.getAsLong());
			}
		} else {
			// SMS/카카오: user_info 에 저장된 해시와 비교
			String stored = ch.info().otpCodeHash();
			if (stored == null || !ch.info().hasValidOtp(channel.name(), LocalDateTime.now())) {
				throw new BusinessException(HttpStatus.UNAUTHORIZED, "OTP_EXPIRED", "인증번호가 만료되었습니다. 다시 요청해 주세요.");
			}
			if (!hasher.matches(user.userId(), cmd.otp(), stored)) {
				throw otpFail(user.userId());
			}
			// 1회용: 해시가 그대로일 때만 비워서, 동시 요청으로 같은 코드를 두 번 쓰지 못하게 한다.
			if (!infos.consumeOtp(user.userId(), stored)) {
				throw otpFail(user.userId());
			}
		}
		// 챌린지 토큰도 일회용: 현재 jti 와 일치할 때만 소진
		if (!infos.consumeMfa(user.userId(), ch.claims().jti())) {
			throw expired();
		}
		String accessToken = finishLogin(user, ch.claims().device());
		return new VerifyResult(
				new LoginResult.SessionResult(user.userId(), user.loginTypeCode(), user.userType(), DeviceState.MATCH),
				accessToken);
	}

	// =====================================================================
	// helpers - 로그인 완료 / 기기
	// =====================================================================

	/** 로그인 최종 성공 처리: 오류 횟수 초기화 → 마지막 로그인 시각 → 기기 등록/갱신 → Access-Token 발급 */
	private String finishLogin(User user, DeviceInfo device) {
		LocalDateTime now = LocalDateTime.now();
		users.resetFailCount(user.userId());
		infos.markLogin(user.userId(), now);
		infos.registerDevice(user.userId(), device, now);
		return tokens.createAccessToken(user.userId(), user.userType(), device.deviceId());
	}

	/**
	 * 기기 상태 판별.
	 * MATCH: 이 사용자에 등록된 기기와 같음 / CHANGED: 다른 기기가 등록돼 있거나 이 기기가 다른 계정 소유 / NONE: 최초
	 */
	private DeviceState deviceState(String userId, DeviceInfo device) {
		UserInfo info = infos.findOrCreate(userId);
		if (info.hasDevice(device.deviceId())) {
			return DeviceState.MATCH;
		}
		if (info.deviceId() != null || infos.existsByDeviceId(device.deviceId())) {
			return DeviceState.CHANGED;
		}
		return DeviceState.NONE;
	}

	/** 기기 정보 필수/형식 검증 + 컬럼 길이에 맞게 절단 */
	private DeviceInfo validateDevice(@Nullable DeviceInfo d) {
		if (d == null || d.deviceId().isBlank() || d.deviceId().length() > 64) {
			throw bad("DEVICE_REQUIRED", "기기 정보가 필요합니다.");
		}
		String platform = d.platform().toUpperCase();
		if (!platform.equals("IOS") && !platform.equals("ANDROID")) {
			throw bad("DEVICE_INVALID", "지원하지 않는 기기입니다.");
		}
		return new DeviceInfo(d.deviceId(), platform, cut(d.osVersion(), 30),
				cut(d.deviceModel(), 60), cut(d.appVersion(), 30));
	}

	// =====================================================================
	// helpers - MFA 챌린지 토큰 (서명된 JWT + user_info.mfa_jti 로 일회용 보장)
	// =====================================================================

	/** 검증을 마친 챌린지 컨텍스트 */
	private record Challenge(MfaClaims claims, User user, UserInfo info) {
	}

	/** 챌린지 토큰 발급. 새 jti 를 저장해 이전에 발급한 토큰은 자동 무효화(최신 1개만 유효). */
	private String issueChallenge(String userId, DeviceInfo device) {
		infos.findOrCreate(userId);
		String jti = UUID.randomUUID().toString();
		infos.issueMfa(userId, jti);
		return tokens.createMfaToken(userId, jti, device, otpProps.challengeTtlSeconds());
	}

	/** 챌린지 토큰 검증: 서명/만료 → 사용자 존재 → 잠금 → 최신 jti 일치. 하나라도 어긋나면 만료 처리. */
	private Challenge loadChallenge(String token) {
		MfaClaims claims = tokens.parseMfaToken(token).orElseThrow(AuthService::expired);
		User user = users.findById(claims.userId()).orElseThrow(AuthService::expired);
		checkLocked(user);
		UserInfo info = infos.findOrCreate(user.userId());
		if (!info.isCurrentMfa(claims.jti())) {
			throw expired(); // 이미 사용했거나 더 새로운 로그인이 발급됨
		}
		return new Challenge(claims, user, info);
	}

	// =====================================================================
	// helpers - 인증/잠금
	// =====================================================================

	/** id/pw 검증. 없는 아이디와 틀린 비밀번호는 같은 응답으로 처리해 아이디 존재 여부를 숨긴다. */
	private User authenticate(String userId, String password) {
		User user = users.findById(userId).orElseThrow(AuthService::invalid);
		checkLocked(user);
		if (!user.passwordMatches(password)) {
			throw failAndLockIfNeeded(userId, remaining -> invalid());
		}
		return user; // 횟수는 최종 로그인 성공(finishLogin) 때만 초기화한다
	}

	private void checkLocked(User user) {
		if (user.isLocked(authProps.maxFailCount())) {
			throw locked();
		}
	}

	/**
	 * id/pw·OTP 오류 공통 처리: 횟수 +1(DB 원자적 증가). 한도에 도달한 바로 그 요청부터 423,
	 * 아니면 호출자가 정한 예외(남은 횟수 전달)를 반환한다.
	 */
	private BusinessException failAndLockIfNeeded(String userId, java.util.function.IntFunction<BusinessException> onRemaining) {
		int count = users.increaseFailCount(userId);
		return count >= authProps.maxFailCount() ? locked() : onRemaining.apply(authProps.maxFailCount() - count);
	}

	/** OTP 오류: 남은 시도 횟수를 data.remainingAttempts 로 알려준다 */
	private BusinessException otpFail(String userId) {
		return failAndLockIfNeeded(userId, remaining -> new BusinessException(HttpStatus.UNAUTHORIZED, "OTP_INVALID",
				"인증번호가 올바르지 않습니다.", ErrorData.remaining(remaining)));
	}

	// =====================================================================
	// helpers - 인증 수단/예외
	// =====================================================================

	/** loginType 에서 허용되는 수단인지 확인하고 enum 으로 변환 */
	private OtpChannel requireChannel(User user, String raw) {
		LoginType type = user.loginType().orElseThrow(AuthService::unsupported);
		OtpChannel channel = OtpChannel.parse(raw).orElseThrow(() -> bad("OTP_CHANNEL_INVALID", "허용되지 않는 인증 수단입니다."));
		List<OtpChannel> allowed = OtpChannel.forLoginType(type);
		if (!allowed.contains(channel)) {
			throw bad("OTP_CHANNEL_INVALID", "허용되지 않는 인증 수단입니다.");
		}
		return channel;
	}

	/** 암호학적으로 안전한 난수로 OTP 숫자열 생성 */
	private String generateCode() {
		// [개발용] 고정 OTP 가 설정되어 있으면 난수 대신 그 값을 쓴다 (해시 저장/검증 흐름은 그대로)
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
	private static String mask(String phone) {
		return phone.length() <= 4 ? "****" : "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
	}

	private static @Nullable String cut(@Nullable String s, int max) {
		return s == null || s.length() <= max ? s : s.substring(0, max);
	}

	/** 챌린지 토큰 만료/소진/무효 → 처음부터 다시 로그인 */
	private static BusinessException expired() {
		return new BusinessException(HttpStatus.UNAUTHORIZED, "CHALLENGE_EXPIRED", "인증 시간이 만료되었습니다. 처음부터 다시 로그인해 주세요.");
	}

	/** 아이디 존재 여부 노출 방지: data 없이 동일 응답 */
	private static BusinessException invalid() {
		return new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다.");
	}

	private static BusinessException locked() {
		return new BusinessException(HttpStatus.LOCKED, "ACCOUNT_LOCKED", "인증 오류가 누적되어 계정이 잠겼습니다. 관리자에게 문의해 주세요.");
	}

	private static BusinessException bad(String code, String message) {
		return new BusinessException(HttpStatus.BAD_REQUEST, code, message);
	}

	private static BusinessException unsupported() {
		return new BusinessException(HttpStatus.FORBIDDEN, "UNSUPPORTED_LOGIN_TYPE", "지원하지 않는 인증 유형입니다.");
	}
}
