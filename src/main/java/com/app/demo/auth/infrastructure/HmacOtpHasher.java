package com.app.demo.auth.infrastructure;

import com.app.demo.auth.config.OtpProperties;
import com.app.demo.auth.domain.OtpHasher;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** 서버 비밀키를 쓰는 HMAC-SHA256 OTP 해시. userId 를 함께 바인딩해 다른 사용자의 해시와 섞이지 않게 한다. */
@Component
class HmacOtpHasher implements OtpHasher {

	private static final String ALGORITHM = "HmacSHA256";

	private final byte[] secret;

	HmacOtpHasher(OtpProperties props) {
		this.secret = props.hashSecret().getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public String hash(String userId, String code) {
		try {
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(new SecretKeySpec(secret, ALGORITHM));
			return HexFormat.of().formatHex(mac.doFinal((userId + ":" + code).getBytes(StandardCharsets.UTF_8)));
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("OTP 해시 계산 실패", e);
		}
	}

	@Override
	public boolean matches(String userId, String code, String storedHash) {
		// 상수 시간 비교 (타이밍 공격 방지)
		return MessageDigest.isEqual(
				hash(userId, code).getBytes(StandardCharsets.UTF_8), storedHash.getBytes(StandardCharsets.UTF_8));
	}
}
