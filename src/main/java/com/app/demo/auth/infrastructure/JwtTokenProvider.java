package com.app.demo.auth.infrastructure;

import com.app.demo.auth.config.JwtProperties;
import com.app.demo.auth.domain.AuthUser;
import com.app.demo.auth.domain.MfaClaims;
import com.app.demo.auth.domain.TokenProvider;
import com.app.demo.user.domain.DeviceInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * JWT(HS256) 기반 {@link TokenProvider} 구현. 같은 키로 두 종류를 발급하며 {@code typ} 클레임으로 구분한다.
 * <ul>
 *   <li>access: 로그인 완료 후 Authorization: Bearer 로 쓰는 토큰</li>
 *   <li>mfa   : id/pw 통과 후 OTP 단계에서만 쓰는 챌린지 토큰 (access 로 사용 불가)</li>
 * </ul>
 */
@Component
class JwtTokenProvider implements TokenProvider {

	private static final String TYP = "typ";

	private final SecretKey key;
	private final long expirationMinutes;

	JwtTokenProvider(JwtProperties props) {
		this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
		this.expirationMinutes = props.expirationMinutes();
	}

	@Override
	public String createAccessToken(String userId, @Nullable Integer userType, String deviceId) {
		Date now = new Date();
		JwtBuilder b = Jwts.builder()
				.subject(userId)
				.claim(TYP, "access")
				.claim("deviceId", deviceId)
				.issuedAt(now)
				.expiration(new Date(now.getTime() + expirationMinutes * 60_000));
		if (userType != null) {
			b.claim("userType", userType);
		}
		return b.signWith(key).compact();
	}

	@Override
	public Optional<AuthUser> parseAccessToken(String token) {
		return parseTyped(token, "access").map(c -> {
			String subject = c.getSubject();
			return subject == null ? null : new AuthUser(subject, c.get("deviceId", String.class));
		});
	}

	@Override
	public String createMfaToken(String userId, String jti, DeviceInfo d, long ttlSeconds) {
		Date now = new Date();
		JwtBuilder b = Jwts.builder()
				.subject(userId)
				.id(jti)
				.claim(TYP, "mfa")
				.claim("did", d.deviceId())
				.claim("plt", d.platform())
				.issuedAt(now)
				.expiration(new Date(now.getTime() + ttlSeconds * 1000));
		// 기기 정보는 토큰에 실어 두어 서버에 별도 저장하지 않는다 (null 값은 제외)
		putIfPresent(b, "osv", d.osVersion());
		putIfPresent(b, "mdl", d.deviceModel());
		putIfPresent(b, "app", d.appVersion());
		return b.signWith(key).compact();
	}

	@Override
	public Optional<MfaClaims> parseMfaToken(String token) {
		return parseTyped(token, "mfa").map(c -> {
			String subject = c.getSubject();
			String jti = c.getId();
			String did = c.get("did", String.class);
			String plt = c.get("plt", String.class);
			if (subject == null || jti == null || did == null || plt == null) {
				return null;
			}
			DeviceInfo device = new DeviceInfo(did, plt,
					c.get("osv", String.class), c.get("mdl", String.class), c.get("app", String.class));
			return new MfaClaims(subject, jti, device);
		});
	}

	/** 서명·만료 검증 후 typ 이 기대값일 때만 클레임을 돌려준다. */
	private Optional<Claims> parseTyped(String token, String typ) {
		try {
			Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
			return typ.equals(claims.get(TYP, String.class)) ? Optional.of(claims) : Optional.empty();
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private static void putIfPresent(JwtBuilder b, String name, @Nullable String value) {
		if (value != null) {
			b.claim(name, value);
		}
	}
}
