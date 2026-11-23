package com.app.demo.home.api;

import com.app.demo.global.security.AuthUser;
import com.app.demo.home.api.dto.HomeSummaryResponse;
import com.app.demo.home.application.HomeService;
import com.app.demo.home.domain.HomeSummary;
import com.app.demo.notice.domain.Notice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 홈 API (Bearer Access-Token 필요) */
@RestController
@RequiredArgsConstructor
@Slf4j
public class HomeController {

	private final HomeService homeService;

	/** 홈 화면에 필요한 요약 값 (현재 요약 수치는 더미) */
	@GetMapping("/api/home/summary")
	public HomeSummaryResponse summary(@AuthenticationPrincipal AuthUser user) {
		log.info("Home summary received");

		HomeSummary summary = homeService.summary(user.getUserId());
		Notice latestNotice = homeService.latestNotice().orElse(null);

		return HomeSummaryResponse.of(summary, latestNotice);
	}
}
