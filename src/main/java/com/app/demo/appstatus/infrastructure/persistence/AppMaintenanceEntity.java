package com.app.demo.appstatus.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** app_maintenance 테이블 매핑 (점검 기간 1건 = 1행, 여러 건 예약 가능, use_flg = 1 인 행만 점검으로 친다) */
@Getter
@Setter
@TableName("app_maintenance")
public class AppMaintenanceEntity {

	/** use_flg 사용 값 */
	public static final int USE = 1;

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;
	private LocalDateTime frDt;
	private LocalDateTime toDt;
	/** 0: 미사용(기본, 미리 등록만), 1: 사용 */
	private Integer useFlg;
	private LocalDateTime createdAt;
}
