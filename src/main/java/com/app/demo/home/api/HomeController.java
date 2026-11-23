package com.app.demo.home.api;

import com.app.demo.global.api.ApiResponse;
import com.app.demo.global.security.AuthUser;
import com.app.demo.home.api.dto.HomeSummaryResponse;
import com.app.demo.home.application.HomeService;
import com.app.demo.home.domain.HomeSummary;
import com.app.demo.notice.domain.Notice;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 홈 API (Bearer Access-Token 필요) */
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

	private final HomeService homeService;

	/** 홈 화면에 필요한 요약 값 (현재 요약 수치는 더미) */
	@GetMapping("/summary")
	public ApiResponse<HomeSummaryResponse> summary(@AuthenticationPrincipal AuthUser user) {
		HomeSummary summary = homeService.summary(user.getUserId());
		Notice latestNotice = homeService.latestNotice().orElse(null);
		HomeSummaryResponse data = HomeSummaryResponse.of(summary, latestNotice);
		return ApiResponse.ok(data);
	}
}
