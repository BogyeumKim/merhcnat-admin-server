-- =====================================================================
-- 스키마: app  (수동 실행용)
-- user 테이블은 회사 운영 테이블이라 수정하지 않는다. 이 파일은 서버가 쓰는 user_info 한 개만 만든다.
-- (MFA 챌린지 토큰은 서명된 JWT 로 발급하고, 일회용 처리는 user_info.mfa_jti 한 컬럼으로 하므로 별도 테이블이 없다)
-- =====================================================================

-- user 와 1:1 (PK 겸 FK). 알림 설정 + 등록 기기 + 인증 진행 상태를 한 행에 보관한다.
-- 기기 정책: 1 사용자 = 1 기기 = 1 사용자. 새 기기가 OTP 를 통과하면 기존 기기 정보를 덮어쓴다.
--           (여러 기기를 동시에 허용하게 되면 기기 컬럼을 별도 테이블로 분리해야 한다)
create table if not exists user_info
(
    user_id                   varchar(30)  not null comment 'PK 겸 FK (user.user_id)' primary key,

    -- ── 알림 설정 ─────────────────────────────────────────────
    -- 보안(security) 알림은 필수라 컬럼 없이 항상 발송한다.
    push_enabled              tinyint(1)   not null default 1 comment '앱 내 알림 마스터 스위치',
    push_notice_enabled       tinyint(1)   not null default 1 comment '공지사항 알림',
    push_transaction_enabled  tinyint(1)   not null default 1 comment '거래내역(승인/취소) 알림',
    push_settlement_enabled   tinyint(1)   not null default 1 comment '정산 알림',

    -- ── 등록 기기 (device_id UNIQUE = 한 기기에는 한 사용자만) ──────────
    device_id                 varchar(64)  null comment '앱이 만든 UUID',
    device_platform           varchar(10)  null comment 'IOS | ANDROID',
    os_version                varchar(30)  null,
    device_model              varchar(60)  null,
    app_version               varchar(30)  null,
    expo_push_token           varchar(255) null comment 'ExponentPushToken[...]',
    expo_push_token_updated_at datetime    null comment '푸시 토큰 갱신 시각 (장기 미갱신 토큰 정리용). OS 알림 권한은 토큰 유무로 판단',

    -- ── 인증 진행 상태 ───────────────────────────────────────
    last_login_at             datetime     null comment '마지막 로그인 완료 시각',
    last_totp_step            bigint       null comment '마지막 사용 TOTP time-step (같은 코드 재사용 방지)',
    mfa_jti                   varchar(36)  null comment '현재 유효한 MFA 챌린지 토큰 ID. 새 로그인이 덮어쓰고 OTP 성공 시 null → 일회용',
    otp_code_hash             char(64)     null comment 'SMS/카카오 OTP 의 HMAC-SHA256. 평문 저장 금지',
    otp_channel               varchar(10)  null comment 'SMS | KAKAO',
    otp_expires_at            datetime     null comment 'OTP 만료 시각',
    otp_sent_at               datetime     null comment '재발송 간격 제한용',

    created_at                datetime     not null default current_timestamp,
    updated_at                datetime     not null default current_timestamp on update current_timestamp,
    constraint fk_user_info_user foreign key (user_id) references `user` (user_id) on delete cascade,
    unique index uq_user_info_device (device_id),
    unique index uq_user_info_push_token (expo_push_token)
) comment 'user 부가정보(알림 설정/기기/인증 상태)';
