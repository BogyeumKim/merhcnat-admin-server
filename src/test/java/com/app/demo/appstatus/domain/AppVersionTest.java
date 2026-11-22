package com.app.demo.appstatus.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class AppVersionTest {

	@ParameterizedTest(name = "{0} < {1}")
	@CsvSource({
			"1.0.0, 1.0.1",
			"1.0.9, 1.1.0",
			"1.9.0, 1.10.0",
			"1.10.0, 2.0.0",
			"0.99.99, 1.0.0"
	})
	@DisplayName("major.minor.patch 를 숫자 단위로 비교한다")
	void comparesNumerically(String lower, String higher) {
		AppVersion a = AppVersion.parse(lower).orElseThrow();
		AppVersion b = AppVersion.parse(higher).orElseThrow();
		assertThat(a.isLowerThan(b)).isTrue();
		assertThat(b.isLowerThan(a)).isFalse();
	}

	@Test
	@DisplayName("같은 버전은 낮지 않다")
	void equalIsNotLower() {
		AppVersion v = AppVersion.parse("1.2.3").orElseThrow();
		assertThat(v.isLowerThan(AppVersion.parse("1.2.3").orElseThrow())).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {"1.2.3-beta.1", "1.2.3+45", " 1.2.3 "})
	@DisplayName("pre-release/build 표기와 앞뒤 공백은 무시하고 1.2.3 으로 본다")
	void ignoresSuffix(String value) {
		assertThat(AppVersion.parse(value)).contains(new AppVersion(1, 2, 3));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "1", "1.2", "1.2.3.4", "v1.2.3", "a.b.c", "1.2.x", "123456.0.0", "1..3"})
	@DisplayName("형식이 틀리면 empty")
	void rejectsMalformed(String value) {
		assertThat(AppVersion.parse(value)).isEmpty();
	}
}
