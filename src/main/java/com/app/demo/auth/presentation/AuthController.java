package com.app.demo.auth.presentation;

import com.app.demo.auth.application.AuthService;
import com.app.demo.auth.application.LoginCommand;
import com.app.demo.auth.application.LoginResult;
import com.app.demo.auth.application.OtpSendCommand;
import com.app.demo.auth.application.OtpSentResult;
import com.app.demo.auth.application.OtpVerifyCommand;
import com.app.demo.auth.application.VerifyResult;
import com.app.demo.global.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인(2단계 인증) API. HTTP 200 은 "실제로 성공"했을 때만 내려간다. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private static final String ACCESS_TOKEN_HEADER = "Access-Token";

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	/**
	 * 1단계: id/pw + 기기정보 검증.
	 * <ul>
	 *   <li>200 SUCCESS      : 로그인 완료(등록된 기기 + loginType=1). Access-Token 헤더 포함</li>
	 *   <li>202 MFA_REQUIRED : id/pw 는 통과했지만 추가 인증 필요. data.challengeToken 으로 다음 단계 진행 (아직 로그인 아님)</li>
	 * </ul>
	 */
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<?>> login(@RequestBody LoginRequest request) {
		LoginResult result = authService.login(new LoginCommand(
				request.userId() == null ? "" : request.userId(),
				request.password() == null ? "" : request.password(),
				request.device() == null ? null : request.device().toDomain()));

		return switch (result) {
			case LoginResult.Completed c -> ResponseEntity.ok()
					.header(ACCESS_TOKEN_HEADER, c.accessToken())
					.body(ApiResponse.ok(AuthSessionResponse.from(c.session())));
			case LoginResult.MfaRequired m -> ResponseEntity.status(HttpStatus.ACCEPTED)
					.body(new ApiResponse<>("MFA_REQUIRED", "추가 인증이 필요합니다.", MfaChallengeResponse.from(m.challenge())));
		};
	}

	/** loginType=1: 카카오/SMS OTP 발송. 발송 성공이면 200 */
	@PostMapping("/otp/send")
	public ApiResponse<OtpSentResponse> sendOtp(@RequestBody OtpSendRequest request) {
		OtpSentResult r = authService.sendOtp(new OtpSendCommand(
				request.challengeToken() == null ? "" : request.challengeToken(),
				request.channel() == null ? "" : request.channel()));
		return ApiResponse.ok(OtpSentResponse.from(r));
	}

	/** OTP 검증. 통과하면 로그인 완료 200 + Access-Token 헤더 (이 시점에 기기가 등록/교체된다) */
	@PostMapping("/otp/verify")
	public ResponseEntity<ApiResponse<AuthSessionResponse>> verifyOtp(@RequestBody OtpVerifyRequest request) {
		VerifyResult r = authService.verifyOtp(new OtpVerifyCommand(
				request.challengeToken() == null ? "" : request.challengeToken(),
				request.channel() == null ? "" : request.channel(),
				request.otp() == null ? "" : request.otp()));
		return ResponseEntity.ok()
				.header(ACCESS_TOKEN_HEADER, r.accessToken())
				.body(ApiResponse.ok(AuthSessionResponse.from(r.session())));
	}
}
