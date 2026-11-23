package com.app.demo.auth.application;

import com.app.demo.auth.config.AuthProperties;
import com.app.demo.auth.domain.AccessToken;
import com.app.demo.auth.domain.Session;
import com.app.demo.auth.domain.SessionStore;
import com.app.demo.auth.exception.AuthErrorCode;
import com.app.demo.global.security.AuthUser;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 사용자당 하나의 로그인 세션(최신 Access-Token 1개만 유효) 관리.
 * <ul>
 *   <li>로그인 최종 성공: 새 토큰의 jti 로 세션을 저장 → 다른 기기(또는 이전) 토큰은 즉시 무효</li>
 *   <li>보호된 요청: 토큰의 jti 가 세션과 같아야 통과하고, 통과할 때마다 무활동 만료를 연장</li>
 *   <li>비밀번호 변경: 세션 삭제 → 모든 기기에서 다시 로그인</li>
 * </ul>
 * 무활동 만료는 auth.session-idle-minutes 이고, Access-Token 만료 시각(jwt.expiration-minutes)을 넘겨 연장하지 않는다.
 * 절대 상한을 바꾸려면 jwt.expiration-minutes 설정만 바꾸면 된다.
 */
@Service
@RequiredArgsConstructor
public class SessionService {

	private final SessionStore store;
	private final AuthProperties authProps;
	private final Clock clock;

	/** 로그인 최종 성공 시 세션 시작(기존 세션 교체) */
	void start(String userId, AccessToken token, String deviceIdHash) {
		Instant now = clock.instant();
		Session session = Session.with()
				.jti(token.getJti())
				.deviceIdHash(deviceIdHash)
				.issuedAt(now)
				.create();
		Duration ttl = ttlUntil(token.getExpiresAt(), now);
		store.save(userId, session, ttl);
	}

	/**
	 * 요청에 쓰인 토큰이 현재 세션의 토큰인지 확인하면서 무활동 만료를 연장한다(저장소 왕복 1번: 조회와 연장을 함께).
	 * <p>jti 가 다른(이미 교체된) 토큰의 요청도 키 만료는 연장되지만 문제없다: 그 키에는 이미 새 세션이 들어 있고,
	 * 연장 폭은 min(무활동 만료, 옛 토큰 남은 수명)이라 새 세션의 토큰 만료를 넘지 않으며, 옛 토큰 자체는 계속 거부된다.
	 * 영향은 옛 토큰이 만료될 때까지(최대 Access-Token 수명) 새 세션의 무활동 타임아웃이 늦춰질 수 있다는 것뿐이다.
	 *
	 * @return 거부 사유. 통과하면 empty (SESSION_EXPIRED: 세션 없음 / SESSION_REPLACED: 다른 로그인으로 교체됨)
	 */
	public Optional<AuthErrorCode> verify(AuthUser user) {
		Duration ttl = ttlUntil(user.getExpiresAt(), clock.instant());
		Optional<Session> session = store.findAndExtend(user.getUserId(), ttl);
		if (session.isEmpty()) {
			return Optional.of(AuthErrorCode.SESSION_EXPIRED);
		}
		if (!session.get().isCurrent(user.getJti())) {
			return Optional.of(AuthErrorCode.SESSION_REPLACED);
		}
		return Optional.empty();
	}

	/** 세션 종료(비밀번호 변경 등). 이후 기존 토큰은 SESSION_EXPIRED */
	void end(String userId) {
		store.delete(userId);
	}

	/** min(무활동 만료, 토큰 남은 수명). 최소 1초 */
	private Duration ttlUntil(Instant tokenExpiresAt, Instant now) {
		Duration idle = Duration.ofMinutes(authProps.getSessionIdleMinutes());
		Duration remaining = Duration.between(now, tokenExpiresAt);
		Duration ttl = remaining.compareTo(idle) < 0 ? remaining : idle;
		return ttl.isNegative() || ttl.isZero() ? Duration.ofSeconds(1) : ttl;
	}
}
