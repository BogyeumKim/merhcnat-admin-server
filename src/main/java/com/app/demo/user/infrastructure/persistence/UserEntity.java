package com.app.demo.user.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 회사 운영 user 테이블 매핑(운영 테이블이라 컬럼 추가 불가, 로컬에는 last_change_pw 가 있는 것으로 가정). user 는 MySQL 예약어라 백틱 사용. */
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
	private LocalDateTime lastChangePw;
	private String oldPassword1;
	private String oldPassword2;
	private String oldPassword3;
	private Integer useType;
	private Integer tmpPassword;
}
