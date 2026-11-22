package com.app.demo.auth.application;

import com.app.demo.auth.config.OtpProperties;
import com.app.demo.auth.domain.MfaClaims;
import com.app.demo.auth.domain.TokenProvider;
import com.app.demo.auth.exception.AuthErrorCode;
import com.app.demo.global.exception.BusinessException;
import com.app.demo.user.domain.DeviceInfo;
import com.app.demo.user.domain.User;
import com.app.demo.user.domain.UserInfo;
import com.app.demo.user.domain.UserInfoRepository;
import com.app.demo.user.domain.UserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 일회용 챌린지 토큰(서명된 JWT + user_info.mfa_jti) 발급·검증·소진.
 * 새로 발급하면 jti 가 바뀌어 이전 토큰은 자동 무효화된다(사용자당 최신 1개만 유효).
 * <ul>
 *   <li>MFA 챌린지: id/pw 통과 후 OTP 단계에서 사용</li>
 *   <li>비밀번호 변경 챌린지: 임시비밀번호 사용자가 새 비밀번호를 등록할 때 사용</li>
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

	String issueMfa(String userId, DeviceInfo device) {
		String jti = newJti(userId);
		return tokens.createMfaToken(userId, jti, device, otpProps.challengeTtlSeconds());
	}

	PasswordChangeChallenge issuePasswordChange(String userId, DeviceInfo device) {
		String jti = newJti(userId);
		return new PasswordChangeChallenge(userId,
				tokens.createPasswordChangeToken(userId, jti, device, otpProps.challengeTtlSeconds()));
	}

	Challenge loadMfa(String token) {
		return load(tokens.parseMfaToken(token));
	}

	Challenge loadPasswordChange(String token) {
		return load(tokens.parsePasswordChangeToken(token));
	}

	/** 현재 jti 와 일치할 때만 소진한다. 동시 요청으로 같은 토큰을 두 번 쓰면 뒤 요청은 만료 처리된다. */
	void consume(Challenge challenge) {
		if (!infos.consumeMfa(challenge.user().userId(), challenge.claims().jti())) {
			throw expired();
		}
	}

	/** 새 jti 를 저장해 이전에 발급한 토큰을 무효화한다. */
	private String newJti(String userId) {
		infos.findOrCreate(userId);
		String jti = UUID.randomUUID().toString();
		infos.issueMfa(userId, jti);
		return jti;
	}

	/** 서명/만료 → 사용자 존재 → 잠금 → 이용 가능 → 최신 jti 일치. 잠금/정지 외에는 모두 만료로 처리한다. */
	private Challenge load(Optional<MfaClaims> parsed) {
		MfaClaims claims = parsed.orElseThrow(ChallengeService::expired);
		User user = users.findById(claims.userId()).orElseThrow(ChallengeService::expired);
		accountLock.checkLocked(user);
		accountLock.checkUsable(user);
		UserInfo info = infos.findOrCreate(user.userId());
		if (!info.isCurrentMfa(claims.jti())) {
			throw expired(); // 이미 사용했거나 더 새로운 로그인이 발급됨
		}
		return new Challenge(claims, user, info);
	}

	/** 서명 오류·만료·소진·더 새로운 로그인 발급 등은 모두 "처음부터 다시 로그인" 으로 안내한다. */
	private static BusinessException expired() {
		return new BusinessException(AuthErrorCode.CHALLENGE_EXPIRED);
	}
}
