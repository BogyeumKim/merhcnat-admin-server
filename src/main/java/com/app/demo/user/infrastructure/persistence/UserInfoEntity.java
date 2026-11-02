package com.app.demo.user.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * user_info 테이블 매핑 (전체 컬럼). 래퍼 타입을 써서 insert 시 null 은 건너뛰고 DB 기본값이 적용되게 한다.
 * 도메인 모델 {@code UserInfo} 는 이 중 인증 판단에 필요한 값만 사용한다.
 */
@Getter
@Setter
@TableName("user_info")
public class UserInfoEntity {

	@TableId(value = "user_id", type = IdType.INPUT)
	private String userId;

	// 알림 설정
	private Boolean pushEnabled;
	private Boolean pushNoticeEnabled;
	private Boolean pushTransactionEnabled;
	private Boolean pushSettlementEnabled;

	// 등록 기기
	private String deviceId;
	private String devicePlatform;
	private String osVersion;
	private String deviceModel;
	private String appVersion;
	private String expoPushToken;
	private LocalDateTime expoPushTokenUpdatedAt;

	// 인증 진행 상태
	private LocalDateTime lastLoginAt;
	private Long lastTotpStep;
	private String mfaJti;
	private String otpCodeHash;
	private String otpChannel;
	private LocalDateTime otpExpiresAt;
	private LocalDateTime otpSentAt;
}
