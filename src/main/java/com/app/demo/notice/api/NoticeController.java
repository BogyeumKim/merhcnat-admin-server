package com.app.demo.notice.api;

import com.app.demo.global.api.ApiResponse;
import com.app.demo.notice.api.dto.NoticePopupResponse;
import com.app.demo.notice.application.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 공지 API (Bearer Access-Token 필요) */
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

	private final NoticeService noticeService;

	/** 로그인 후 팝업으로 띄울 공지 목록 (우선순위 순) */
	@GetMapping("/popup")
	public ApiResponse<NoticePopupResponse> popup() {
		return ApiResponse.ok(NoticePopupResponse.from(noticeService.popups()));
	}
}
