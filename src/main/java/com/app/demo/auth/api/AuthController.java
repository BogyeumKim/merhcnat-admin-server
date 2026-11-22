package com.app.demo.auth.api;

import com.app.demo.auth.api.dto.AuthSessionResponse;
import com.app.demo.auth.api.dto.AuthStepResponse;
import com.app.demo.auth.api.dto.LoginRequest;
import com.app.demo.auth.api.dto.MfaChallengeResponse;
import com.app.demo.auth.api.dto.OtpSendRequest;
import com.app.demo.auth.api.dto.OtpSentResponse;
import com.app.demo.auth.api.dto.OtpVerifyRequest;
import com.app.demo.auth.api.dto.PasswordChangeRequiredResponse;
import com.app.demo.auth.api.dto.TempPasswordChangeRequest;
import com.app.demo.auth.application.LoginService;
import com.app.demo.auth.application.LoginStep;
import com.app.demo.auth.application.OtpService;
import com.app.demo.auth.application.TempPasswordService;
import com.app.demo.global.api.ApiResponse;
import com.app.demo.global.security.AuthAuditContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인(2단계 인증) API. 각 단계가 성공하면 HTTP 200 이고 code 로 다음 단계를 구분한다. 실패만 4xx/5xx 다.
 * <ul>
 *   <li>SUCCESS                  : 로그인 완료. Access-Token 응답 헤더 포함</li>
 *   <li>MFA_REQUIRED             : id/pw 통과, OTP 필요. data.challengeToken 으로 otp/send·otp/verify 진행</li>
 *   <li>PASSWORD_CHANGE_REQUIRED : 임시비밀번호. data.challengeToken 으로 {@code /password/temp} 호출</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final LoginService loginService;
	private final OtpService otpService;
	private final TempPasswordService tempPasswordService;

	/** 1단계: id/pw + 기기정보 검증 */
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthStepResponse>> login(@Valid @RequestBody LoginRequest request) {
		return toResponse(loginService.login(request.toCommand()));
	}

	/** 카카오/SMS/이메일 OTP 발송 */
	@PostMapping("/otp/send")
	public ApiResponse<OtpSentResponse> sendOtp(@Valid @RequestBody OtpSendRequest request) {
		return ApiResponse.ok(OtpSentResponse.from(otpService.sendOtp(request.toCommand())));
	}

	/** OTP 검증. 통과하면 로그인 완료 (이 시점에 기기가 등록/교체된다) */
	@PostMapping("/otp/verify")
	public ResponseEntity<ApiResponse<AuthStepResponse>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
		return toResponse(otpService.verifyOtp(request.toCommand()));
	}

	/**
	 * 임시비밀번호 변경 (challengeToken 필요, Bearer 불필요). 성공하면 로그인 완료.
	 * 400 CURRENT_PASSWORD_INVALID(5회 합산 잠금) / PASSWORD_POLICY_VIOLATION, 409 PASSWORD_REUSED, 401 CHALLENGE_EXPIRED, 423 ACCOUNT_LOCKED
	 */
	@PostMapping("/password/temp")
	public ResponseEntity<ApiResponse<AuthStepResponse>> changeTempPassword(
			@Valid @RequestBody TempPasswordChangeRequest request) {
		return toResponse(tempPasswordService.changeTempPassword(request.toCommand()));
	}

	/** 단계 결과 → 200 응답. 로그인 완료일 때만 Access-Token 헤더를 붙인다. */
	private static ResponseEntity<ApiResponse<AuthStepResponse>> toResponse(LoginStep step) {
		return switch (step) {
			case LoginStep.Completed(var session, var accessToken) ->
					AccessTokenHeader.ok(accessToken, ApiResponse.ok(AuthSessionResponse.from(session)));
			case LoginStep.MfaRequired(var challenge) -> ResponseEntity.ok(nextStep(AuthStepCode.MFA_REQUIRED,
					AuthStepCode.MFA_REQUIRED.getMessage(), MfaChallengeResponse.from(challenge)));
			case LoginStep.PasswordChangeRequired(var challenge) -> ResponseEntity.ok(nextStep(AuthStepCode.PASSWORD_CHANGE_REQUIRED,
					AuthStepCode.passwordChangeMessage(challenge.reason()), PasswordChangeRequiredResponse.from(challenge)));
		};
	}

	private static ApiResponse<AuthStepResponse> nextStep(AuthStepCode code, String message, AuthStepResponse data) {
		AuthAuditContext.result(code.name()); // 200 이지만 로그인 미완료 단계임을 감사 로그에 구분해 남긴다
		return ApiResponse.of(code.name(), message, data);
	}
}
