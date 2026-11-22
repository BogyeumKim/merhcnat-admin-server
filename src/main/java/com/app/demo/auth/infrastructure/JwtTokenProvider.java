package com.app.demo.auth.infrastructure;

import com.app.demo.auth.config.JwtProperties;
import com.app.demo.auth.domain.AccessToken;
import com.app.demo.auth.domain.MfaClaims;
import com.app.demo.auth.domain.TokenProvider;
import com.app.demo.global.security.AuthUser;
import com.app.demo.global.security.Sha256;
import com.app.demo.user.domain.DeviceInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * JWT(HS256) 기반 {@link TokenProvider} 구현. 세 종류를 발급하며 서로 대체할 수 없다.
 * <ul>
 *   <li>access: 로그인 완료 후 Authorization: Bearer 로 쓰는 토큰 (access 키, aud=access)</li>
 *   <li>mfa   : id/pw 통과 후 OTP 단계에서만 쓰는 챌린지 토큰 (challenge 키, aud=mfa)</li>
 *   <li>pwc   : 비밀번호 변경 단계에서만 쓰는 챌린지 토큰 (challenge 키, aud=pwc)</li>
 * </ul>
 * 검증 시 서명 키·aud·typ 이 모두 기대값이어야 한다. kid 헤더는 키 교체 대비용이다.
 * Access-Token 에는 기기 ID 원문 대신 SHA-256 해시(dvh)만 싣는다. 챌린지 토큰은 기기 등록에 필요한 기기 정보를
 * 원문으로 싣지만 수명이 짧고(최대 15분) 로그인 완료 전에만 쓰인다.
 */
@Component
class JwtTokenProvider implements TokenProvider {

	private static final String TYP = "typ";
	private static final String ACCESS = "access";
	private static final String MFA = "mfa";
	private static final String PASSWORD_CHANGE = "pwc";
	private static final String ACCESS_KID = "access-v1";
	private static final String CHALLENGE_KID = "challenge-v1";

	private final SecretKey accessKey;
	private final SecretKey challengeKey;
	private final Duration accessTokenTtl;
	private final Clock clock;

	JwtTokenProvider(JwtProperties props, Clock clock) {
		this.accessKey = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
		this.challengeKey = Keys.hmacShaKeyFor(props.challengeSecret().getBytes(StandardCharsets.UTF_8));
		this.accessTokenTtl = Duration.ofMinutes(props.expirationMinutes());
		this.clock = clock;
	}

	@Override
	public AccessToken createAccessToken(String userId, @Nullable Integer userType, String deviceId) {
		Instant now = clock.instant();
		Instant expiresAt = now.plus(accessTokenTtl);
		String jti = UUID.randomUUID().toString();
		JwtBuilder b = Jwts.builder()
				.header().keyId(ACCESS_KID).and()
				.subject(userId)
				.id(jti)
				.audience().add(ACCESS).and()
				.claim(TYP, ACCESS)
				.claim("dvh", Sha256.hex(deviceId))
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt));
		if (userType != null) {
			b.claim("userType", userType);
		}
		return new AccessToken(b.signWith(accessKey).compact(), jti, expiresAt);
	}

	@Override
	public Optional<AuthUser> parseAccessToken(String token) {
		return parse(token, accessKey, ACCESS).map(c -> {
			String subject = c.getSubject();
			String jti = c.getId();
			Date exp = c.getExpiration();
			if (subject == null || jti == null || exp == null) {
				return null;
			}
			return new AuthUser(subject, c.get("dvh", String.class), jti, exp.toInstant());
		});
	}

	@Override
	public String createMfaToken(String userId, String jti, DeviceInfo d, long ttlSeconds) {
		return createChallenge(MFA, userId, jti, d, ttlSeconds);
	}

	@Override
	public String createPasswordChangeToken(String userId, String jti, DeviceInfo d, long ttlSeconds) {
		return createChallenge(PASSWORD_CHANGE, userId, jti, d, ttlSeconds);
	}

	private String createChallenge(String type, String userId, String jti, DeviceInfo d, long ttlSeconds) {
		Instant now = clock.instant();
		JwtBuilder b = Jwts.builder()
				.header().keyId(CHALLENGE_KID).and()
				.subject(userId)
				.id(jti)
				.audience().add(type).and()
				.claim(TYP, type)
				.claim("did", d.deviceId())
				.claim("plt", d.platform())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(ttlSeconds)));
		// 기기 정보는 토큰에 실어 두어 서버에 별도 저장하지 않는다 (null 값은 제외)
		putIfPresent(b, "osv", d.osVersion());
		putIfPresent(b, "mdl", d.deviceModel());
		putIfPresent(b, "app", d.appVersion());
		return b.signWith(challengeKey).compact();
	}

	@Override
	public Optional<MfaClaims> parseMfaToken(String token) {
		return parseChallenge(token, MFA);
	}

	@Override
	public Optional<MfaClaims> parsePasswordChangeToken(String token) {
		return parseChallenge(token, PASSWORD_CHANGE);
	}

	private Optional<MfaClaims> parseChallenge(String token, String type) {
		return parse(token, challengeKey, type).map(c -> {
			String subject = c.getSubject();
			String jti = c.getId();
			String did = c.get("did", String.class);
			String plt = c.get("plt", String.class);
			Date exp = c.getExpiration();
			if (subject == null || jti == null || did == null || plt == null || exp == null) {
				return null;
			}
			DeviceInfo device = new DeviceInfo(did, plt,
					c.get("osv", String.class), c.get("mdl", String.class), c.get("app", String.class));
			return new MfaClaims(subject, jti, device, LocalDateTime.ofInstant(exp.toInstant(), clock.getZone()));
		});
	}

	/** 서명(키)·만료·aud 를 검증하고, typ 까지 기대값일 때만 클레임을 돌려준다. */
	private Optional<Claims> parse(String token, SecretKey key, String type) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(key)
					.requireAudience(type)
					.clock(() -> Date.from(clock.instant()))
					.build()
					.parseSignedClaims(token)
					.getPayload();
			return type.equals(claims.get(TYP, String.class)) ? Optional.of(claims) : Optional.empty();
		} catch (JwtException | IllegalArgumentException _) {
			return Optional.empty();
		}
	}

	private static void putIfPresent(JwtBuilder b, String name, @Nullable String value) {
		if (value != null) {
			b.claim(name, value);
		}
	}
}
