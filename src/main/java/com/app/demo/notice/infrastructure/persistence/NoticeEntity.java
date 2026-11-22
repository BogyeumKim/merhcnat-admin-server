package com.app.demo.notice.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** notice 테이블 매핑 (MyBatis/Lombok 엔티티라 NullAway 검사에서 제외된 패키지) */
@Getter
@Setter
@TableName("notice")
public class NoticeEntity {

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;
	private String title;
	private String body;
	private String imageUrl;
	private Integer imageWidth;
	private Integer imageHeight;
	private String imageAlt;
	private Integer priority;
	private Boolean popup;
	private Boolean active;
	private LocalDateTime startAt;
	private LocalDateTime endAt;
	private LocalDateTime createdAt;
}
