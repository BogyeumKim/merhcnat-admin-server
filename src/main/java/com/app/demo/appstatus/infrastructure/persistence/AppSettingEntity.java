package com.app.demo.appstatus.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** app_setting 테이블 매핑: 운영 설정 키-값 (예: IOS_MIN_VERSION=1.0.0) */
@Getter
@Setter
@TableName("app_setting")
public class AppSettingEntity {

	@TableId(value = "setting_key", type = IdType.INPUT)
	private String settingKey;
	private String settingValue;
	private String description;
	private LocalDateTime updatedAt;
}
