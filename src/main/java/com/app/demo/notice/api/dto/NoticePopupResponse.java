package com.app.demo.notice.api.dto;

import com.app.demo.notice.domain.Notice;
import com.app.demo.notice.domain.NoticeImage;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/** GET /api/notices/popup 응답 본문(200). 공지가 없으면 notices 는 빈 배열. */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NoticePopupResponse {

	private final List<Item> notices;

	public static NoticePopupResponse from(List<Notice> notices) {
		List<Item> items = notices.stream().map(Item::from).toList();
		return NoticePopupResponse.with()
				.notices(items)
				.create();
	}

	/** image 가 없으면 응답에서 제외, 사진 한 장 공지는 body 가 빈 문자열 */
	@JsonPropertyOrder({"id", "title", "body", "image"})
	@Getter
	@Builder(builderMethodName = "with", buildMethodName = "create")
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class Item {

		private final String id;

		private final String title;

		private final String body;

		private final @Nullable Image image;

		static Item from(Notice n) {
			NoticeImage noticeImage = n.getImage();
			Image image = noticeImage == null ? null : Image.from(noticeImage);
			return Item.with()
					.id(n.getId())
					.title(n.getTitle())
					.body(n.getBody())
					.image(image)
					.create();
		}
	}

	@JsonPropertyOrder({"url", "width", "height", "alt"})
	@Getter
	@Builder(builderMethodName = "with", buildMethodName = "create")
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class Image {

		private final String url;

		private final int width;

		private final int height;

		private final @Nullable String alt;

		static Image from(NoticeImage i) {
			return Image.with()
					.url(i.getUrl())
					.width(i.getWidth())
					.height(i.getHeight())
					.alt(i.getAlt())
					.create();
		}
	}
}
