package com.app.demo.auth.domain;

import static com.app.demo.auth.domain.PasswordPolicy.CHARSET;
import static com.app.demo.auth.domain.PasswordPolicy.CONTAINS_USER_ID;
import static com.app.demo.auth.domain.PasswordPolicy.LENGTH;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyTest {

	private static final String USER_ID = "merchant01";

	@Test
	@DisplayName("규칙을 모두 지키면 통과한다")
	void passesWhenAllRulesMet() {
		assertThat(PasswordPolicy.violations(USER_ID, "Abcd1234!")).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"Ab1!", "Abcdefgh1234567890!@#"})
	@DisplayName("길이가 8자 미만이거나 20자 초과면 LENGTH")
	void lengthOutOfRange(String password) {
		assertThat(PasswordPolicy.violations(USER_ID, password)).containsExactly(LENGTH);
	}

	@ParameterizedTest
	@ValueSource(strings = {"12345678!", "Abcdefgh!", "Abcd12345"})
	@DisplayName("영문·숫자·특수문자 중 하나라도 없으면 CHARSET")
	void missingCharacterType(String password) {
		assertThat(PasswordPolicy.violations(USER_ID, password)).containsExactly(CHARSET);
	}

	@ParameterizedTest
	@ValueSource(strings = {"가나다라1234!", "abcd 1234!", "Abcd1234！", "Abcd1234😀"})
	@DisplayName("허용되지 않은 문자(한글·공백·전각·이모지)가 있으면 CHARSET")
	void disallowedCharacter(String password) {
		assertThat(PasswordPolicy.violations(USER_ID, password)).containsExactly(CHARSET);
	}

	@ParameterizedTest
	@ValueSource(strings = {"merchant01!A", "MERCHANT01!a", "xMerChant01#"})
	@DisplayName("아이디를 포함하면 대소문자와 관계없이 CONTAINS_USER_ID")
	void containsUserIdIgnoringCase(String password) {
		assertThat(PasswordPolicy.violations(USER_ID, password)).containsExactly(CONTAINS_USER_ID);
	}

	@Test
	@DisplayName("여러 규칙을 어기면 정해진 순서로 중복 없이 알려준다")
	void reportsViolationsInStableOrder() {
		assertThat(PasswordPolicy.violations("ab", "ab가")).containsExactly(LENGTH, CHARSET, CONTAINS_USER_ID);
	}
}
