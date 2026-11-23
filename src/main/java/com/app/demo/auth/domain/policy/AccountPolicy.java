package com.app.demo.auth.domain.policy;

import com.app.demo.auth.exception.AuthErrorCode;
import com.app.demo.global.exception.ErrorCode;
import com.app.demo.user.domain.User;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/** 이용할 수 있는 계정인지 가른다. 삭제 → 정지 → 사용 아님 순으로 처음 걸린 사유. 실패 횟수에는 합산하지 않는다. */
@UtilityClass
public class AccountPolicy {

	/** ex) 삭제 계정 → {@code INVALID_CREDENTIALS}(없는 계정과 같은 401), 정지 계정 → {@code ACCOUNT_SUSPENDED}(400), 사용 아님 → {@code LOGIN_NOT_ALLOWED}(400) */
	public Optional<ErrorCode> checkUsable(User user) {
		if (user.isDeleted()) {
			return Optional.of(AuthErrorCode.INVALID_CREDENTIALS);
		}
		if (user.isSuspended()) {
			return Optional.of(AuthErrorCode.ACCOUNT_SUSPENDED);
		}
		// 기존 MMS 처럼 사용(0)만 받는다. NULL·코드표에 없는 값을 사용으로 보면 막아 둔 계정이 열린다.
		if (!user.isActive()) {
			return Optional.of(AuthErrorCode.LOGIN_NOT_ALLOWED);
		}
		return Optional.empty();
	}
}
