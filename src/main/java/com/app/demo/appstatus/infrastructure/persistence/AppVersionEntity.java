package com.app.demo.appstatus.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** app_version 테이블 매핑 (필수 버전 출시 1건 = 1행, 이력으로 남기고 지우지 않는다. use_flg = 1 인 행만 적용) */
@Getter
@Setter
@TableName("app_version")
public class AppVersionEntity {

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;
	/** IOS | ANDROID */
	private String platform;
	private String version;
	/** 이 시각부터 필수 버전으로 적용 */
	private LocalDateTime forceDt;
	/** 0: 미사용(기본, 미리 등록만), 1: 사용 */
	private Integer useFlg;
	private String memo;
	private LocalDateTime createdAt;
}
