package com.app.demo.notice.infrastructure.persistence;

import com.app.demo.notice.domain.Notice;
import com.app.demo.notice.domain.NoticeImage;
import com.app.demo.notice.domain.NoticeRepository;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** NoticeRepository 의 MyBatis-Plus 구현 */
@Repository
@RequiredArgsConstructor
class NoticeRepositoryImpl implements NoticeRepository {

	private final NoticeMapper mapper;
	private final Clock clock;

	@Override
	public List<Notice> findPopups(LocalDateTime now) {
		return mapper.selectList(visible(now).eq("popup", true).orderByAsc("priority").orderByDesc("id")).stream()
				.map(this::toDomain).toList();
	}

	@Override
	public Optional<Notice> findLatest(LocalDateTime now) {
		return mapper.selectList(visible(now).orderByDesc("created_at").orderByDesc("id").last("limit 1")).stream()
				.findFirst().map(this::toDomain);
	}

	/** 활성 + 노출 기간(시작/종료 null 은 제한 없음) 안의 공지 조건 */
	private static QueryWrapper<NoticeEntity> visible(LocalDateTime now) {
		return new QueryWrapper<NoticeEntity>().eq("active", true)
				.and(w -> w.isNull("start_at").or().le("start_at", now))
				.and(w -> w.isNull("end_at").or().ge("end_at", now));
	}

	private Notice toDomain(NoticeEntity e) {
		NoticeImage image = null;
		if (e.getImageUrl() != null && e.getImageWidth() != null && e.getImageHeight() != null) {
			image = NoticeImage.with()
					.url(e.getImageUrl())
					.width(e.getImageWidth())
					.height(e.getImageHeight())
					.alt(e.getImageAlt())
					.create();
		}
		LocalDate createdOn = e.getCreatedAt() == null ? LocalDate.now(clock) : e.getCreatedAt().toLocalDate();
		String id = String.valueOf(e.getId());
		String title = Objects.requireNonNullElse(e.getTitle(), "");
		String body = Objects.requireNonNullElse(e.getBody(), "");
		return Notice.with()
				.id(id)
				.title(title)
				.body(body)
				.image(image)
				.createdOn(createdOn)
				.create();
	}
}
