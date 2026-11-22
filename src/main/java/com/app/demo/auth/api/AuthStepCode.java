package com.app.demo.auth.api;

import com.app.demo.user.domain.PasswordChangeReason;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 로그인 중간 단계의 성공 응답 code/message. 단계가 성공하면 HTTP 200 이고 앱은 이 code 로 다음 화면을 정한다. */
@Getter
@RequiredArgsConstructor
enum AuthStepCode {

	MFA_REQUIRED("추가 인증이 필요합니다."),
	PASSWORD_CHANGE_REQUIRED("비밀번호를 변경해야 합니다. 새 비밀번호를 등록해 주세요.");

	private final String message;

	/** 비밀번호 변경 안내 문구를 이유에 맞게 고른다(앱은 분기에 쓰지 않는 참고 문구). */
	static String passwordChangeMessage(PasswordChangeReason reason) {
		return switch (reason) {
			case TEMP_PASSWORD -> "임시비밀번호입니다. 새 비밀번호를 등록해 주세요.";
			case PASSWORD_EXPIRED -> "비밀번호 사용 기간이 지났습니다. 새 비밀번호를 등록해 주세요.";
		};
	}
}
