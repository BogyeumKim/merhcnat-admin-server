package com.app.demo.auth.api;

import com.app.demo.auth.api.dto.AuthStepResponse;
import com.app.demo.auth.api.dto.LoginRequest;
import com.app.demo.auth.api.dto.OtpSendRequest;
import com.app.demo.auth.api.dto.OtpSentResponse;
import com.app.demo.auth.api.dto.OtpVerifyRequest;
import com.app.demo.auth.api.dto.TempPasswordChangeRequest;
import com.app.demo.auth.application.LoginCommand;
import com.app.demo.auth.application.LoginService;
import com.app.demo.auth.application.LoginStep;
import com.app.demo.auth.application.OtpSendCommand;
import com.app.demo.auth.application.OtpSentResult;
import com.app.demo.auth.application.OtpService;
import com.app.demo.auth.application.OtpVerifyCommand;
import com.app.demo.auth.application.TempPasswordChangeCommand;
import com.app.demo.auth.application.TempPasswordService;
import com.app.demo.global.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인(2단계 인증) API. 각 단계가 성공하면 HTTP 200 이고 code 로 다음 단계를 구분한다. 실패만 4xx/5xx 다.
 * <ul>
 *   <li>SUCCESS                  : 로그인 완료. data.accessToken 을 이후 요청의 Bearer 토큰으로 쓴다</li>
 *   <li>MFA_REQUIRED             : id/pw 통과, OTP 필요. data.challengeToken 으로 otp/send·otp/verify 진행</li>
 *   <li>PASSWORD_CHANGE_REQUIRED : 임시비밀번호. data.challengeToken 으로 {@code /password/temp} 호출</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {

	private final LoginService loginService;
	private final OtpService otpService;
	private final TempPasswordService tempPasswordService;

	/** 1단계: id/pw + 기기정보 검증 */
	@PostMapping(value = "/api/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ApiResponse<AuthStepResponse> login(@Valid @RequestBody LoginRequest request) {
		log.info("Login received");

		LoginCommand command = request.toCommand();
		LoginStep step = loginService.login(command);

		return LoginStepResponse.from(step);
	}

	/** 카카오/SMS/이메일 OTP 발송 */
	@PostMapping(value = "/api/auth/otp/send", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ApiResponse<OtpSentResponse> sendOtp(@Valid @RequestBody OtpSendRequest request) {
		log.info("OTP send received");

		OtpSendCommand command = request.toCommand();
		OtpSentResult result = otpService.sendOtp(command);

		OtpSentResponse data = OtpSentResponse.from(result);
		return ApiResponse.ok(data);
	}

	/** OTP 검증. 통과하면 로그인 완료 (이 시점에 기기가 등록/교체된다) */
	@PostMapping(value = "/api/auth/otp/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ApiResponse<AuthStepResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
		log.info("OTP verify received");

		OtpVerifyCommand command = request.toCommand();
		LoginStep step = otpService.verifyOtp(command);

		return LoginStepResponse.from(step);
	}

	/**
	 * 임시비밀번호 변경 (challengeToken 필요, Bearer 불필요). 성공하면 로그인 완료.
	 * 400 CURRENT_PASSWORD_INVALID(5회 합산 잠금) / PASSWORD_POLICY_VIOLATION, 409 PASSWORD_REUSED, 401 CHALLENGE_EXPIRED, 423 ACCOUNT_LOCKED
	 */
	@PostMapping(value = "/api/auth/password/temp", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ApiResponse<AuthStepResponse> changeTempPassword(@Valid @RequestBody TempPasswordChangeRequest request) {
		log.info("Temp password change received");

		TempPasswordChangeCommand command = request.toCommand();
		LoginStep step = tempPasswordService.changeTempPassword(command);

		return LoginStepResponse.from(step);
	}
}
