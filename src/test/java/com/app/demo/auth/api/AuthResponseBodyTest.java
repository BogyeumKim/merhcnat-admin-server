package com.app.demo.auth.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.demo.auth.application.LoginService;
import com.app.demo.auth.application.LoginStep;
import com.app.demo.auth.application.PasswordService;
import com.app.demo.auth.application.SessionResult;
import com.app.demo.auth.domain.AccessToken;
import com.app.demo.auth.domain.Session;
import com.app.demo.auth.domain.SessionStore;
import com.app.demo.auth.domain.TokenProvider;
import com.app.demo.global.security.AuthUser;
import com.app.demo.user.domain.DeviceState;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 토큰은 응답 헤더가 아니라 본문(data.accessToken)으로 내려간다. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthResponseBodyTest {

	private static final String USER = "body_token_test";
	private static final String LOGIN_BODY = """
			{"userId":"body_token_test","password":"Test1234!","device":{"deviceId":"body-device","platform":"IOS"}}
			""";

	@Autowired
	MockMvc mvc;

	@Autowired
	TokenProvider tokens;

	@Autowired
	SessionStore sessions;

	@MockitoBean
	LoginService loginService;

	@MockitoBean
	PasswordService passwordService;

	@AfterEach
	void logout() {
		sessions.delete(USER);
	}

	@Test
	@DisplayName("로그인 완료(SUCCESS)는 data.accessToken 으로 토큰을 주고 Access-Token 헤더는 없다")
	void loginSuccessPutsTokenInBody() throws Exception {
		SessionResult session = SessionResult.with().userId(USER).loginType(1).userType(1).deviceState(DeviceState.MATCH).create();
		given(loginService.login(any())).willReturn(LoginStep.Completed.of(session, "issued.jwt.token"));

		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("SUCCESS"))
				.andExpect(jsonPath("$.data.userId").value(USER))
				.andExpect(jsonPath("$.data.accessToken").value("issued.jwt.token"))
				.andExpect(header().doesNotExist("Access-Token"));
	}

	@Test
	@DisplayName("비밀번호 변경 성공은 data.accessToken 으로 새 토큰을 주고 Access-Token 헤더는 없다")
	void passwordChangePutsNewTokenInBody() throws Exception {
		AccessToken current = tokens.createAccessToken(USER, 1, "body-device");
		sessions.save(USER, Session.with().jti(current.getJti()).issuedAt(Instant.now()).create(), Duration.ofMinutes(5));
		given(passwordService.change(any(AuthUser.class), anyString(), anyString()))
				.willReturn(AccessToken.with().value("new.jwt.token").jti("new-jti").expiresAt(Instant.now().plusSeconds(60)).create());

		mvc.perform(put("/api/users/me/password").header("Authorization", "Bearer " + current.getValue())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"currentPassword\":\"Test1234!\",\"newPassword\":\"Abcd1234!\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("SUCCESS"))
				.andExpect(jsonPath("$.data.accessToken").value("new.jwt.token"))
				.andExpect(header().doesNotExist("Access-Token"));
	}
}
