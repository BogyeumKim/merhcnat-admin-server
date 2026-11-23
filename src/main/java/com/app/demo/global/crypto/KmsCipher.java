package com.app.demo.global.crypto;

import org.jspecify.annotations.Nullable;

/**
 * KMS 로 암호화된 컬럼(*_ENC)의 복호화. 기존 MMS 와 같은 키·방식으로 읽기 위해 둔다.
 *
 * <p>운영 구현은 MMS 의 kms-crypt jar({@code CryptUtils})를 쓴다. jar 설정이 JVM 전역 static 이라 빈 하나에서만 configure 한다.
 * <pre>{@code
 * @Component
 * class CryptUtilsKmsCipher implements KmsCipher {
 *     private final CryptUtils crypt;
 *
 *     CryptUtilsKmsCipher(KmsProperties p) {
 *         CryptUtils.configure(p.getSendIp(), p.getRemoteIp(), p.getRemotePort(), p.getRemoteTimeout());
 *         this.crypt = new CryptUtils();
 *     }
 *
 *     public @Nullable String decrypt(@Nullable String encrypted) {
 *         if (encrypted == null || encrypted.isBlank()) return null;
 *         try {
 *             return crypt.decrypt(encrypted);
 *         } catch (Exception e) {
 *             throw new IllegalStateException("KMS 복호화 실패", e);
 *         }
 *     }
 * }
 * }</pre>
 * jar 가 들어오기 전까지는 {@link PlainTextKmsCipher} 가 값을 그대로 돌려준다.
 */
public interface KmsCipher {

	/** 암호문을 복호화한다. null·공백이면 null 이다. */
	@Nullable String decrypt(@Nullable String encrypted);
}
