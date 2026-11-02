package com.app.demo.auth.infrastructure;

import com.app.demo.auth.domain.TotpVerifier;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.OptionalLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** RFC 6238 TOTP (HMAC-SHA1, 30초, 6자리). 시계 오차 ±1 step 허용. */
@Component
class Rfc6238TotpVerifier implements TotpVerifier {

	/** 코드가 바뀌는 주기(초) */
	private static final long STEP_SECONDS = 30L;
	private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

	@Override
	public OptionalLong verify(String base32Secret, String code) {
		if (code.length() != 6 || !code.chars().allMatch(Character::isDigit)) {
			return OptionalLong.empty();
		}
		byte[] key = decodeBase32(base32Secret);
		if (key == null) {
			return OptionalLong.empty();
		}
		long current = System.currentTimeMillis() / 1000 / STEP_SECONDS;
		for (long step = current - 1; step <= current + 1; step++) {
			if (MessageDigest.isEqual(generate(key, step).getBytes(StandardCharsets.UTF_8), code.getBytes(StandardCharsets.UTF_8))) {
				return OptionalLong.of(step);
			}
		}
		return OptionalLong.empty();
	}

	/** RFC 4226 HOTP 계산: HMAC-SHA1 → 동적 절삭 → 6자리 */
	private static String generate(byte[] key, long step) {
		try {
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(new SecretKeySpec(key, "HmacSHA1"));
			byte[] h = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
			int o = h[h.length - 1] & 0x0f;
			int bin = ((h[o] & 0x7f) << 24) | ((h[o + 1] & 0xff) << 16) | ((h[o + 2] & 0xff) << 8) | (h[o + 3] & 0xff);
			return String.format("%06d", bin % 1_000_000);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("TOTP 계산 실패", e);
		}
	}

	/** Base32(RFC 4648) 시크릿 → 바이트. 허용되지 않는 문자가 있으면 null */
	private static byte @Nullable [] decodeBase32(String s) {
		String clean = s.toUpperCase(Locale.ROOT).replace("=", "").replace(" ", "");
		int buffer = 0;
		int bits = 0;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (char c : clean.toCharArray()) {
			int v = BASE32.indexOf(c);
			if (v < 0) {
				return null;
			}
			buffer = (buffer << 5) | v;
			bits += 5;
			if (bits >= 8) {
				out.write((buffer >> (bits - 8)) & 0xff);
				bits -= 8;
			}
		}
		return out.toByteArray();
	}
}
