package com.app.demo.auth.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.app.demo.auth.exception.AuthErrorCode;
import com.app.demo.user.domain.UseType;
import com.app.demo.user.domain.User;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountPolicyTest {

	@Test
	@DisplayName("이용 상태가 NULL 이거나 코드표에 없으면 LOGIN_NOT_ALLOWED 로 막는다")
	void rejectsUnknownUseType() {
		assertThat(AccountPolicy.checkUsable(user(UseType.fromCode(null)))).contains(AuthErrorCode.LOGIN_NOT_ALLOWED);
		assertThat(AccountPolicy.checkUsable(user(UseType.fromCode(7)))).contains(AuthErrorCode.LOGIN_NOT_ALLOWED);
		assertThat(AccountPolicy.checkUsable(user(UseType.ACTIVE))).isEmpty();
	}

	private static User user(UseType useType) {
		return User.with().userId("testuser01").oldPasswords(List.of()).useType(useType).create();
	}
}
