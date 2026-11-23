package com.app.demo.notice.api;

import com.app.demo.global.api.ApiResponse;
import com.app.demo.notice.api.dto.NoticePopupResponse;
import com.app.demo.notice.application.NoticeService;
import com.app.demo.notice.domain.Notice;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 공지 API (Bearer Access-Token 필요) */
@RestController
@RequiredArgsConstructor
@Slf4j
public class NoticeController {

	private final NoticeService noticeService;

	/** 로그인 후 팝업으로 띄울 공지 목록 (우선순위 순) */
	@GetMapping("/api/notices/popup")
	public ApiResponse<NoticePopupResponse> popup() {
		log.info("Notice popup received");

		List<Notice> notices = noticeService.popups();

		NoticePopupResponse data = NoticePopupResponse.from(notices);
		return ApiResponse.ok(data);
	}
}
