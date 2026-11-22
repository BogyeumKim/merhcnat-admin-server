package com.app.demo.auth.api;

import com.app.demo.auth.api.dto.AuthSessionResponse;
import com.app.demo.auth.api.dto.LoginRequest;
import com.app.demo.auth.api.dto.OtpSendRequest;
import com.app.demo.auth.api.dto.OtpSentResponse;
import com.app.demo.auth.api.dto.OtpVerifyRequest;
import com.app.demo.auth.api.dto.TempPasswordChangeRequest;
import com.app.demo.auth.application.LoginService;
import com.app.demo.auth.application.LoginSuccess;
import com.app.demo.auth.application.OtpService;
import com.app.demo.auth.application.TempPasswordService;
import com.app.demo.global.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인(2단계 인증) API. HTTP 200 은 "실제로 로그인/발송에 성공"했을 때만 내려가고, 그 외는 모두 4xx/5xx 다.
 * <ul>
 *   <li>401 MFA_REQUIRED             : id/pw 는 통과했지만 OTP 필요. data.challengeToken 으로 다음 단계 진행</li>
 *   <li>403 PASSWORD_CHANGE_REQUIRED : 임시비밀번호. data.challengeToken 으로 {@code /password/temp} 호출</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private static final String ACCESS_TOKEN_HEADER = "Access-Token";

	private final LoginService loginService;
	private final OtpService otpService;
	private final TempPasswordService tempPasswordService;

	/** 1단계: id/pw + 기기정보 검증. 등록 기기 + loginType=1 이면 바로 200 + Access-Token 헤더 */
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthSessionResponse>> login(@Valid @RequestBody LoginRequest request) {
		return loggedIn(loginService.login(request.toCommand()));
	}

	/** loginType=1: 카카오/SMS OTP 발송. 발송 성공이면 200 */
	@PostMapping("/otp/send")
	public ApiResponse<OtpSentResponse> sendOtp(@Valid @RequestBody OtpSendRequest request) {
		return ApiResponse.ok(OtpSentResponse.from(otpService.sendOtp(request.toCommand())));
	}

	/** OTP 검증. 통과하면 로그인 완료 200 + Access-Token 헤더 (이 시점에 기기가 등록/교체된다) */
	@PostMapping("/otp/verify")
	public ResponseEntity<ApiResponse<AuthSessionResponse>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
		return loggedIn(otpService.verifyOtp(request.toCommand()));
	}

	/**
	 * 임시비밀번호 변경 (challengeToken 필요, Bearer 불필요). 성공하면 로그인이 완료되어 200 + Access-Token 헤더.
	 * 400 CURRENT_PASSWORD_INVALID(5회 합산 잠금) / PASSWORD_POLICY_VIOLATION, 409 PASSWORD_REUSED, 401 CHALLENGE_EXPIRED, 423 ACCOUNT_LOCKED
	 */
	@PostMapping("/password/temp")
	public ResponseEntity<ApiResponse<AuthSessionResponse>> changeTempPassword(
			@Valid @RequestBody TempPasswordChangeRequest request) {
		return loggedIn(tempPasswordService.changeTempPassword(request.toCommand()));
	}

	/** 로그인 완료: 200 + Access-Token 헤더 */
	private static ResponseEntity<ApiResponse<AuthSessionResponse>> loggedIn(LoginSuccess success) {
		return ResponseEntity.ok()
				.header(ACCESS_TOKEN_HEADER, success.accessToken())
				.body(ApiResponse.ok(AuthSessionResponse.from(success.session())));
	}
}
