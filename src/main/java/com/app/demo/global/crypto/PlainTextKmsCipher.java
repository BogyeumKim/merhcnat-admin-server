package com.app.demo.global.crypto;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * kms-crypt jar 가 들어오기 전까지 쓰는 임시 구현. 로컬 DB 의 *_ENC 컬럼에는 평문이 들어 있어 값을 그대로 돌려준다.
 * jar 가 오면 {@link KmsCipher} Javadoc 의 {@code CryptUtilsKmsCipher} 로 바꾸고 이 클래스를 지운다.
 */
@Component
class PlainTextKmsCipher implements KmsCipher {

	@Override
	public @Nullable String decrypt(@Nullable String encrypted) {
		if (encrypted == null || encrypted.isBlank()) {
			return null;
		}
		return encrypted;
	}
}
