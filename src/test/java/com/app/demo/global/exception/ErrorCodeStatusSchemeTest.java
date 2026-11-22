package com.app.demo.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.app.demo.appstatus.exception.AppStatusErrorCode;
import com.app.demo.auth.exception.AuthErrorCode;
import com.app.demo.user.exception.UserErrorCode;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * HTTP status 체계 고정: status 는 앱의 공통 행동만 나눈다.
 * 비즈니스 오류 400 / 인증·세션 401 / 업데이트 필요 403 / 점검·장애 503. 구체적인 화면은 code 로 분기한다.
 */
class ErrorCodeStatusSchemeTest {

	@Test
	@DisplayName("인증·사용자 맥락의 비즈니스 오류는 400 또는 401 만 쓴다")
	void businessErrorsUseOnly400Or401() {
		Stream<ErrorCode> business = Stream.concat(Arrays.stream(AuthErrorCode.values()), Arrays.stream(UserErrorCode.values()));

		assertThat(business.map(ErrorCode::getStatus))
				.allMatch(status -> status == HttpStatus.BAD_REQUEST || status == HttpStatus.UNAUTHORIZED);
	}

	@Test
	@DisplayName("업데이트 필요는 403, 점검은 503")
	void appStatusErrors() {
		assertThat(AppStatusErrorCode.UPDATE_REQUIRED.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(AppStatusErrorCode.MAINTENANCE.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	@DisplayName("426 같은 특수 status 는 쓰지 않는다")
	void noSpecialStatuses() {
		Stream<ErrorCode> all = Stream.of(AuthErrorCode.values(), UserErrorCode.values(), AppStatusErrorCode.values(),
				CommonErrorCode.values()).flatMap(Arrays::stream);

		assertThat(all.map(code -> code.getStatus().value()))
				.doesNotContain(409, 423, 426, 429);
	}
}
