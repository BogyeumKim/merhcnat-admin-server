package com.app.demo.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordHashTest {

	// 기존 MMS 와 같은 계산(Base64(SHA-512(Base64(MD5(원문)))))을 다른 구현으로 미리 구한 값
	private static final String HASH_OF_TEST1234 =
			"AyOWtPaHjQBPav/aHblo/aqkme3rj6dHKLnRYTXU97lrLZRWii2olR0iGXONBwNAd91Pss6MWsfHM/vSbg4yQQ==";

	@Test
	@DisplayName("기존 MMS 가 저장한 해시와 같은 값을 만든다")
	void matchesLegacyMmsHash() {
		assertThat(PasswordHash.of("Test1234!")).isEqualTo(HASH_OF_TEST1234);
	}

	@Test
	@DisplayName("저장된 해시와 원문을 비교하고, 이전 비밀번호 해시와 같으면 재사용으로 본다")
	void comparesByHash() {
		User user = User.with()
				.userId("testuser01")
				.password(HASH_OF_TEST1234)
				.oldPasswords(List.of(PasswordHash.of("Old1234!")))
				.useType(UseType.ACTIVE)
				.create();

		assertThat(user.passwordMatches("Test1234!")).isTrue();
		assertThat(user.passwordMatches(HASH_OF_TEST1234)).isFalse();
		assertThat(user.passwordReused("Old1234!")).isTrue();
		assertThat(user.passwordReused("New1234!")).isFalse();
	}
}
