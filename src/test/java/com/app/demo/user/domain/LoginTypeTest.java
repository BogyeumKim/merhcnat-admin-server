package com.app.demo.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.DisplayName;

class LoginTypeTest {

	@ParameterizedTest
	@CsvSource({"1", "2"})
	@DisplayName("기존 MMS 의 이메일(1)·SMS(2) 인증 유형은 둘 다 MESSAGE 로 받는다")
	void legacyEmailAndSmsAreMessage(int code) {
		assertThat(LoginType.fromCode(code)).contains(LoginType.MESSAGE);
	}
}
