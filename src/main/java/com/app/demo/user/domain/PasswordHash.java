package com.app.demo.user.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import lombok.experimental.UtilityClass;

/**
 * 비밀번호 해시. 기존 MMS 앱이 저장한 값과 그대로 비교하려고 같은 방식(MD5 → Base64 → SHA-512 → Base64)을 쓴다.
 * 솔트 없는 빠른 해시라 레거시 호환 목적으로만 유지한다. 추후 bcrypt 로 옮긴다
 * (Spring Security {@code DelegatingPasswordEncoder} 로 옛 형식을 읽고, 로그인 성공 시 새 형식으로 다시 저장).
 */
@UtilityClass
public class PasswordHash {

	private static final String MD5 = "MD5";
	private static final String SHA_512 = "SHA-512";

	/** ex) 원문 → {@code Base64(SHA-512(Base64(MD5(원문))))} 88자 */
	public String of(String raw) {
		byte[] md5 = digest(MD5, raw.getBytes(StandardCharsets.UTF_8));
		String md5Base64 = Base64.getEncoder().encodeToString(md5);
		byte[] sha512 = digest(SHA_512, md5Base64.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(sha512);
	}

	private byte[] digest(String algorithm, byte[] input) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
			return messageDigest.digest(input);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("JDK 에 " + algorithm + " 이 없다", e);
		}
	}
}
