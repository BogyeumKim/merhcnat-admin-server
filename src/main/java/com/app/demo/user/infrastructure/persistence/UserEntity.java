package com.app.demo.user.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/** 회사 운영 user 테이블 매핑(수정 불가 테이블, 기존 컬럼만). user 는 MySQL 예약어라 백틱 사용. */
@Getter
@Setter
@TableName("`user`")
public class UserEntity {

	@TableId(value = "user_id", type = IdType.INPUT)
	private String userId;
	private String password;
	private Integer loginType;
	private Integer userType;
	private Integer passwordFailCount;
	private String phoneNumber;
	private String totpSecret;
}
