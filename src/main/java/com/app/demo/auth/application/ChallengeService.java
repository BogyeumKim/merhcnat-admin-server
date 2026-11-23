package com.app.demo.auth.application;

import com.app.demo.auth.config.OtpProperties;
import com.app.demo.auth.domain.IdMasking;
import com.app.demo.auth.domain.MfaClaims;
import com.app.demo.auth.domain.TokenProvider;
import com.app.demo.auth.exception.AuthErrorCode;
import com.app.demo.global.exception.BusinessException;
import com.app.demo.global.security.AuthAuditContext;
import com.app.demo.global.security.Sha256;
import com.app.demo.user.domain.DeviceInfo;
import com.app.demo.user.domain.PasswordChangeReason;
import com.app.demo.user.domain.User;
import com.app.demo.user.domain.UserInfo;
import com.app.demo.user.domain.UserInfoRepository;
import com.app.demo.user.domain.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 일회용 챌린지 토큰(서명된 JWT + user_info.mfa_jti) 발급·검증·소진.
 * 새로 발급하면 jti 가 바뀌어 이전 토큰은 자동 무효화된다(사용자당 최신 1개만 유효).
 * <ul>
 *   <li>MFA 챌린지: id/pw 통과 후 OTP 단계에서 사용. 기본 5분이고 OTP 를 발송할 때마다 연장되며(user_info.mfa_expires_at),
 *       토큰 자체의 만료(로그인 시점 + 15분)가 절대 상한이다.</li>
 *   <li>비밀번호 변경 챌린지: 새 비밀번호를 등록할 때 사용. 고정 5분.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
class ChallengeService {

	private final UserRepository users;
	private final UserInfoRepository infos;
	private final TokenProvider tokens;
	private final OtpProperties otpProps;
	private final AccountLockService accountLock;
	private final Clock clock;

	String issueMfa(String userId, DeviceInfo device) {
		String jti = newJti(userId);
		return tokens.createMfaToken(userId, jti, device, otpProps.getChallengeMaxSeconds());
	}

	PasswordChangeChallenge issuePasswordChange(String userId, DeviceInfo device, PasswordChangeReason reason) {
		String jti = newJti(userId);
		String token = tokens.createPasswordChangeToken(userId, jti, device, otpProps.getChallengeTtlSeconds());
		return PasswordChangeChallenge.with()
				.userId(userId)
				.challengeToken(token)
				.reason(reason)
				.create();
	}

	Challenge loadMfa(String token) {
		Optional<MfaClaims> parsed = tokens.parseMfaToken(token);
		return load(parsed);
	}

	Challenge loadPasswordChange(String token) {
		Optional<MfaClaims> parsed = tokens.parsePasswordChangeToken(token);
		return load(parsed);
	}

	/** 현재 jti 와 일치할 때만 소진한다. 동시 요청으로 같은 토큰을 두 번 쓰면 뒤 요청은 만료 처리된다. */
	void consume(Challenge challenge) {
		if (!infos.consumeMfa(challenge.getUser().getUserId(), challenge.getClaims().getJti())) {
			throw expired();
		}
	}

	/** 새 jti 와 기본 유효 시각(now + challenge-ttl)을 저장해 이전에 발급한 토큰을 무효화한다. */
	private String newJti(String userId) {
		infos.findOrCreate(userId);
		String jti = UUID.randomUUID().toString();
		LocalDateTime expiresAt = LocalDateTime.now(clock).plusSeconds(otpProps.getChallengeTtlSeconds());
		infos.issueChallenge(userId, jti, expiresAt);
		return jti;
	}

	/** 서명/만료 → 사용자 존재 → 잠금 → 이용 가능 → 최신 jti·유효 시각. 잠금/정지 외에는 모두 만료로 처리한다. */
	private Challenge load(Optional<MfaClaims> parsed) {
		MfaClaims claims = parsed.orElseThrow(ChallengeService::expired);
		String maskedUserId = IdMasking.mask(claims.getUserId());
		String deviceIdHash = Sha256.hex(claims.getDevice().getDeviceId());
		AuthAuditContext.subject(maskedUserId, deviceIdHash);
		User user = users.findById(claims.getUserId()).orElseThrow(ChallengeService::expired);
		accountLock.checkLocked(user);
		accountLock.checkUsable(user);
		UserInfo info = infos.findOrCreate(user.getUserId());
		if (!info.isCurrentChallenge(claims.getJti(), LocalDateTime.now(clock))) {
			throw expired(); // 이미 사용했거나, 더 새로운 로그인이 발급됐거나, 유효 시각이 지남
		}
		return Challenge.with()
				.claims(claims)
				.user(user)
				.info(info)
				.create();
	}

	/** 서명 오류·만료·소진·더 새로운 로그인 발급 등은 모두 "처음부터 다시 로그인" 으로 안내한다. */
	private static BusinessException expired() {
		return new BusinessException(AuthErrorCode.CHALLENGE_EXPIRED);
	}
}
