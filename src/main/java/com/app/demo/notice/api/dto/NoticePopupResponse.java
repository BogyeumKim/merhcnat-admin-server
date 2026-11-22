package com.app.demo.notice.api.dto;

import com.app.demo.notice.domain.Notice;
import com.app.demo.notice.domain.NoticeImage;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** GET /api/notices/popup 응답 data. 공지가 없으면 notices 는 빈 배열. */
public record NoticePopupResponse(List<Item> notices) {

	public static NoticePopupResponse from(List<Notice> notices) {
		return new NoticePopupResponse(notices.stream().map(Item::from).toList());
	}

	/** image 가 없으면 응답에서 제외, 사진 한 장 공지는 body 가 빈 문자열 */
	public record Item(String id, String title, String body, @Nullable Image image) {

		static Item from(Notice n) {
			NoticeImage i = n.image();
			return new Item(n.id(), n.title(), n.body(), i == null ? null : new Image(i.url(), i.width(), i.height(), i.alt()));
		}
	}

	public record Image(String url, int width, int height, @Nullable String alt) {
	}
}
