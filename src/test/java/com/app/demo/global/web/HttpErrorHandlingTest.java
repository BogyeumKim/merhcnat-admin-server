package com.app.demo.global.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.demo.auth.domain.AccessToken;
import com.app.demo.auth.domain.Session;
import com.app.demo.auth.domain.SessionStore;
import com.app.demo.auth.domain.TokenProvider;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공격자가 임의의 경로·메서드·헤더·본문을 보냈을 때 항상 공통 봉투 + 올바른 4xx 가 나가는지 검증한다.
 * 비인증 요청은 경로 존재 여부와 상관없이 401, 인증된 요청은 404/405/406/415/400/413 으로 구분된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(HttpErrorHandlingTest.ProbeController.class)
class HttpErrorHandlingTest {

	private static final String USER = "probe_user_test";

	@Autowired
	MockMvc mvc;

	@Autowired
	TokenProvider tokens;

	@Autowired
	SessionStore sessions;

	private String bearer;

	/** 필수 쿼리 파라미터 누락·타입 불일치를 검증하기 위한 테스트 전용 엔드포인트(인증 필요 경로) */
	@TestConfiguration
	@RestController
	static class ProbeController {

		@GetMapping("/api/test-probe/param")
		String param(@RequestParam int n) {
			return String.valueOf(n);
		}
	}

	@BeforeEach
	void login() {
		AccessToken token = tokens.createAccessToken(USER, 1, "probe-device");
		sessions.save(USER, new Session(token.jti(), null, Instant.now()), Duration.ofMinutes(5));
		bearer = "Bearer " + token.value();
	}

	@AfterEach
	void logout() {
		sessions.delete(USER);
	}

	// ── 비인증: 어떤 경로·메서드든 401 (경로 존재 여부를 숨긴다) ──

	@Test
	void unauthenticatedRequestsAreAlways401() throws Exception {
		for (String path : new String[] {"/", "/error", "/actuator/health", "/favicon.ico", "/index.html", "/api/nope",
				"/api/home/summary"}) {
			expectEnvelope(mvc.perform(get(path)), 401, "UNAUTHORIZED");
		}
		expectEnvelope(mvc.perform(delete("/api/home/summary")), 401, "UNAUTHORIZED");
		expectEnvelope(mvc.perform(post("/api/nope").contentType(MediaType.APPLICATION_JSON).content("{}")), 401, "UNAUTHORIZED");
	}

	// ── 인증된 요청 ──

	@Test
	void unknownPathIs404() throws Exception {
		expectEnvelope(mvc.perform(auth(get("/api/nope"))), 404, "NOT_FOUND");
		expectEnvelope(mvc.perform(auth(get("/error"))), 404, "NOT_FOUND"); // /error 직접 호출
	}

	@Test
	void wrongMethodIs405WithAllowHeader() throws Exception {
		expectEnvelope(mvc.perform(auth(delete("/api/home/summary"))), 405, "METHOD_NOT_ALLOWED")
				.andExpect(header().string("Allow", containsString("GET")));
	}

	@Test
	void unsupportedContentTypeIs415() throws Exception {
		expectEnvelope(mvc.perform(auth(put("/api/devices/me")).contentType(MediaType.TEXT_PLAIN).content("x")),
				415, "UNSUPPORTED_MEDIA_TYPE");
	}

	@Test
	void unacceptableAcceptIs406AndStillJson() throws Exception {
		expectEnvelope(mvc.perform(auth(get("/api/home/summary")).accept(MediaType.APPLICATION_XML)), 406, "NOT_ACCEPTABLE");
	}

	@Test
	void malformedBodiesAre400() throws Exception {
		expectEnvelope(mvc.perform(auth(put("/api/devices/me")).contentType(MediaType.APPLICATION_JSON).content("{bad")),
				400, "BAD_REQUEST");
		expectEnvelope(mvc.perform(auth(put("/api/devices/me")).contentType(MediaType.APPLICATION_JSON)), 400, "BAD_REQUEST");
		expectEnvelope(mvc.perform(auth(put("/api/devices/me")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"deviceSecret\":{\"a\":1}}")), 400, "BAD_REQUEST");
	}

	@Test
	void missingOrMistypedParamIs400() throws Exception {
		expectEnvelope(mvc.perform(auth(get("/api/test-probe/param"))), 400, "BAD_REQUEST");
		expectEnvelope(mvc.perform(auth(get("/api/test-probe/param").param("n", "abc"))), 400, "BAD_REQUEST");
	}

	// ── 공개 엔드포인트(/api/auth/**) ──

	@Test
	void publicEndpointsGetSameTreatment() throws Exception {
		expectEnvelope(mvc.perform(get("/api/auth/login")), 405, "METHOD_NOT_ALLOWED")
				.andExpect(header().string("Allow", containsString("POST")));
		expectEnvelope(mvc.perform(post("/api/auth/login").contentType(MediaType.TEXT_PLAIN).content("x")),
				415, "UNSUPPORTED_MEDIA_TYPE");
		expectEnvelope(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{bad")),
				400, "BAD_REQUEST");
		expectEnvelope(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_XML).content("{}")), 400, "BAD_REQUEST");
	}

	// ── 본문 크기 ──

	@Test
	void oversizedBodyIs413() throws Exception {
		String big = "{\"userId\":\"" + "a".repeat(70 * 1024) + "\"}";
		expectEnvelope(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(big)),
				413, "PAYLOAD_TOO_LARGE");
	}

	// ── 방화벽(StrictHttpFirewall) 거부 ──

	@Test
	void firewallRejectionsAre400Envelope() throws Exception {
		for (String uri : new String[] {"/api//home/summary", "/api/home;x=1/summary", "/api/%2e%2e/home"}) {
			expectEnvelope(mvc.perform(get("/").with(r -> {
				r.setRequestURI(uri);
				return r;
			})), 400, "BAD_REQUEST");
		}
	}

	private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder) {
		return builder.header("Authorization", bearer);
	}

	private static ResultActions expectEnvelope(ResultActions result, int status, String code) throws Exception {
		return result.andExpect(status().is(status))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.code").value(code))
				.andExpect(jsonPath("$.message").isString());
	}
}
