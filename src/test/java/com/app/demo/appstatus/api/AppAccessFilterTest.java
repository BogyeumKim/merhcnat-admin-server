package com.app.demo.appstatus.api;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.demo.appstatus.application.AppStatusSnapshot;
import com.app.demo.appstatus.domain.AppPlatform;
import com.app.demo.appstatus.domain.AppStatus;
import com.app.demo.appstatus.domain.AppVersion;
import com.app.demo.appstatus.domain.MaintenanceWindow;
import com.app.demo.appstatus.domain.VersionRelease;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 점검 기간(503)·필수 버전(403)·버전 헤더 검증(400)이 로그인 전/후 모든 /api 요청에 적용되는지 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
class AppAccessFilterTest {

	/** 이미 적용 중인 iOS 필수 버전 2.0.0 */
	private static final VersionRelease IOS_MIN_2 =
			VersionRelease.with()
					.platform(AppPlatform.IOS)
					.version(AppVersion.parse("2.0.0").orElseThrow())
					.forceAt(LocalDateTime.of(2026, 1, 1, 0, 0))
					.create();

	@Autowired
	MockMvc mvc;

	@Autowired
	Clock clock;

	@MockitoBean
	AppStatusSnapshot snapshot;

	private void stubStatus(MaintenanceWindow... windows) {
		given(snapshot.current()).willReturn(AppStatus.of(List.of(windows), List.of(IOS_MIN_2)));
	}

	/** 지금 기준 (분 단위) 점검 기간 */
	private MaintenanceWindow windowFromNow(long fromMinutes, long toMinutes) {
		LocalDateTime now = LocalDateTime.now(clock).withNano(0);
		return MaintenanceWindow.of(now.plusMinutes(fromMinutes), now.plusMinutes(toMinutes));
	}

	private String iso(LocalDateTime time) {
		return time.atZone(clock.getZone()).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

	@Test
	@DisplayName("점검 기간이면 로그인 전 API 도 503 MAINTENANCE (startedAt·endsAt 은 ISO-8601 + 오프셋)")
	void maintenanceOnPublicApi() throws Exception {
		MaintenanceWindow window = windowFromNow(-30, 120);
		stubStatus(window);
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("MAINTENANCE"))
				.andExpect(jsonPath("$.message").value("서비스 점검 중입니다. 잠시 후 다시 이용해 주세요."))
				.andExpect(jsonPath("$.data.startedAt").value(iso(window.getFrom())))
				.andExpect(jsonPath("$.data.endsAt").value(iso(window.getTo())))
				.andExpect(jsonPath("$.data.impact").doesNotExist());
	}

	@Test
	@DisplayName("점검 기간이면 로그인 후 API 도 503")
	void maintenanceOnProtectedApi() throws Exception {
		stubStatus(windowFromNow(-1, 1));
		mvc.perform(get("/api/home/summary"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("MAINTENANCE"));
	}

	@Test
	@DisplayName("예약만 돼 있거나 이미 끝난 점검이면 통과 (다음 단계인 인증 검사로 401)")
	void outsideWindowPasses() throws Exception {
		stubStatus(windowFromNow(10, 60), windowFromNow(-60, -10));
		mvc.perform(get("/api/home/summary")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("점검과 낮은 버전이 겹치면 점검(503)이 우선한다")
	void maintenanceWinsOverUpdate() throws Exception {
		stubStatus(windowFromNow(-30, 120));
		mvc.perform(get("/api/home/summary").header("X-App-Platform", "IOS").header("X-App-Version", "1.0.0"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("MAINTENANCE"));
	}

	@Test
	@DisplayName("필수 버전보다 낮으면 403 UPDATE_REQUIRED (data 는 minAppVersion 만)")
	void updateRequired() throws Exception {
		stubStatus();
		mvc.perform(post("/api/auth/login").header("X-App-Platform", "ios").header("X-App-Version", "1.9.9")
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("UPDATE_REQUIRED"))
				.andExpect(jsonPath("$.data.minAppVersion").value("2.0.0"))
				.andExpect(jsonPath("$.data.latestAppVersion").doesNotExist());
	}

	@Test
	@DisplayName("최소 버전 이상이면 통과 (다음 단계인 인증 검사로 401)")
	void sufficientVersionPasses() throws Exception {
		stubStatus();
		mvc.perform(get("/api/home/summary").header("X-App-Platform", "IOS").header("X-App-Version", "2.0.0"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("버전 헤더가 둘 다 없으면 통과 (기존 클라이언트 호환)")
	void noHeadersPass() throws Exception {
		stubStatus();
		mvc.perform(get("/api/home/summary")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("적용 중인 필수 버전이 없는 플랫폼은 통과")
	void platformWithoutPolicyPasses() throws Exception {
		stubStatus();
		mvc.perform(get("/api/home/summary").header("X-App-Platform", "ANDROID").header("X-App-Version", "0.0.1"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("헤더가 한쪽만 있거나 형식이 틀리면 400 BAD_REQUEST")
	void malformedHeaders() throws Exception {
		stubStatus();
		mvc.perform(get("/api/home/summary").header("X-App-Version", "1.0.0"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
		mvc.perform(get("/api/home/summary").header("X-App-Platform", "IOS"))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/api/home/summary").header("X-App-Platform", "WEB").header("X-App-Version", "1.0.0"))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/api/home/summary").header("X-App-Platform", "IOS").header("X-App-Version", "latest"))
				.andExpect(status().isBadRequest());
	}
}
