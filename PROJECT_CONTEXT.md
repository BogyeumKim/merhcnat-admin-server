# demo 프로젝트 컨텍스트 (대화 요약)

앱(모바일, 별도 `app` 세션) 로그인을 위한 서버. 처음에는 Kotlin 프로젝트(`app-migration-server`)로 만들었고,
이후 Java 로 새로 만든 이 `demo` 프로젝트로 옮겨 재구현했다. 이 문서는 그동안의 결정과 계약을 한곳에 모은 것이다.

## 1. 기술 스택 / 컨벤션

- Spring Boot **4.0.0 이상 고정**, Java 25 toolchain, Gradle
- MyBatis-Plus: `com.baomidou:mybatis-plus-spring-boot4-starter:3.5.18.1`
- 설정은 YAML (`application.yaml`), 로컬 MySQL 스키마 `app`, `root` 계정, 비밀번호 없음
- null 안전성: **JSpecify `@Nullable` + NullAway**(Error Prone). `package-info.java`/`@NullMarked` 는 쓰지 않고,
  `build.gradle` 의 `NullAway:AnnotatedPackages=com.app.demo` 로 전체를 "@Nullable 없으면 non-null" 규칙으로 컴파일 시 검사(위반 = 컴파일 오류).
  `user.infrastructure.persistence`(MyBatis/Lombok 엔티티)는 `UnannotatedSubPackages` 로 제외, 테스트 코드는 NullAway 비활성
- DDD: 컨텍스트별 `domain / application / infrastructure / presentation`
- 코드 주석은 한글로 상세히 작성
- (참고) Kotlin 은 2.2.x 가 JVM 24 까지만 지원해서 Java 25 와 충돌했었고, 이 프로젝트는 Java 라서 해당 없음

## 2. 패키지 구조 (`com.app.demo`)

| 패키지 | 역할 |
|---|---|
| `global.api` | `ApiResponse`(공통 봉투), `ErrorData` |
| `global.exception` | `BusinessException`, `GlobalExceptionHandler` |
| `global.config` | `PersistenceConfig` (`@MapperScan`) |
| `user.domain` | `User`, `UserInfo`, `LoginType`, `DeviceInfo`, `DeviceState`, `UserRepository`, `UserInfoRepository` |
| `user.infrastructure.persistence` | `UserEntity`, `UserInfoEntity`, 매퍼, 저장소 구현 |
| `auth.config` | `AuthProperties`, `OtpProperties`, `JwtProperties` (`@ConfigurationProperties`) |
| `auth.domain` | `OtpChannel`, `AuthUser`, `MfaClaims`, 포트(`TokenProvider`, `OtpSender`, `OtpHasher`, `TotpVerifier`) |
| `auth.application` | `AuthService`(유스케이스), 커맨드/결과 |
| `auth.infrastructure` | `JwtTokenProvider`, `HmacOtpHasher`, `Rfc6238TotpVerifier`, `LoggingOtpSender`, `JwtAuthFilter`, `SecurityConfig` |
| `auth.presentation` | `AuthController`, 요청/응답 레코드 |
| `device.application` / `device.presentation` | 푸시 토큰·알림 허용 갱신 |

의존 방향: presentation → application → domain ← infrastructure

## 3. 로그인 요구사항 (사용자 지시)

- `user.login_type` = 0: TOTP(인증앱), 1: 카카오/SMS
- 로그인 후 `login_type=0` 은 **항상 OTP 화면**, `1` 은 카카오/문자 선택 버튼 → OTP 입력
- OTP 까지 통과해야 로그인 완료
- OTP 만 단독 전송 금지(무차별 대입 방어) → 처음엔 id/pw 를 매번 같이 보냈고, 이후 **challengeToken 방식**으로 변경(사용자 승인)
- **등록된 기기(MATCH) + `login_type=1` 은 OTP 생략**(사용자 승인). `login_type=0` 은 항상 OTP
- 기기는 **1 사용자 = 1 기기 = 1 사용자**. 다른 계정이 OTP 까지 통과하면 기기 소유자를 덮어쓰고 이전 계정의 푸시 토큰은 해제(사용자 승인)
- **잠금**: id/pw 오류와 OTP 오류를 합쳐 `user.password_fail_count` +1, 5 가 되면 영구 잠금(관리자 해제). 올바른 비밀번호여도 거부.
  카운트는 최종 로그인 성공 때만 0 으로 초기화
- 알려진 부작용: 타인이 남의 아이디로 5번 틀리면 계정이 잠김 → 정책으로 수용 여부를 사용자에게 알림(결정 대기)

## 4. API 계약 (app 세션과 합의 완료)

모든 응답은 `{ code, message, data? }` 봉투. **HTTP 200 은 실제 성공일 때만** 사용하고, 그 외는 다른 status + 같은 봉투.

### POST `/api/auth/login`
요청: `{ userId, password, device: { deviceId(필수,≤64), platform("IOS"|"ANDROID", 필수), osVersion?, deviceModel?, appVersion? } }`
- **200 SUCCESS**: 로그인 완료(등록 기기 + loginType=1). `Access-Token` 헤더 + `data: { userId, loginType, userType, deviceState:"MATCH" }`
- **202 MFA_REQUIRED**: id/pw 통과, 추가 인증 필요(아직 로그인 아님).
  `data: { userId, loginType, userType, deviceState:"NONE"|"MATCH"|"CHANGED", channels:[...], challengeToken }`
  (loginType 0 → `["TOTP"]`, 1 → `["KAKAO","SMS"]`)
- 401 `INVALID_CREDENTIALS` (없는 아이디와 틀린 비밀번호 동일 응답, data 없음)
- 423 `ACCOUNT_LOCKED` (영구, 관리자 문의) / 400 `DEVICE_REQUIRED`·`DEVICE_INVALID` / 403 `UNSUPPORTED_LOGIN_TYPE`

### POST `/api/auth/otp/send` (loginType=1)
요청: `{ challengeToken, channel:"KAKAO"|"SMS" }`
- 200 `data: { expiresInSeconds(180), resendAfterSeconds(60), maskedPhone }`
- 429 `OTP_RESEND_TOO_SOON`(`data.retryAfterSeconds`), 409 `PHONE_NOT_REGISTERED`, 400 `OTP_CHANNEL_INVALID`, 401 `CHALLENGE_EXPIRED`, 423 `ACCOUNT_LOCKED`

### POST `/api/auth/otp/verify`
요청: `{ challengeToken, channel:"TOTP"|"KAKAO"|"SMS", otp }`
- 200 + `Access-Token` 헤더, `data: { userId, loginType, userType, deviceState:"MATCH" }` (이 시점에 기기 등록/교체)
- 401 `OTP_INVALID`(`data.remainingAttempts`), 401 `OTP_EXPIRED`(재발송), 401 `CHALLENGE_EXPIRED`(처음부터 다시),
  409 `TOTP_NOT_ENROLLED`(앱은 "관리자에게 문의" 안내), 423 `ACCOUNT_LOCKED`

### PUT `/api/devices/me` (Bearer Access-Token)
요청: `{ expoPushToken? }` (보낸 필드만 갱신. 예전 `notificationsEnabled` 는 제거, 보내도 무시됨)
- 200 SUCCESS / 400 `PUSH_TOKEN_INVALID`·`DEVICE_REQUIRED` / 404 `DEVICE_NOT_FOUND` / 401 `UNAUTHORIZED`
- 토큰 형식은 `ExponentPushToken[...]` 또는 `ExpoPushToken[...]` 만 허용

### 공통 오류
400 `BAD_REQUEST`(본문 형식), 401 `UNAUTHORIZED`(Bearer 없음/무효), 500 `INTERNAL_ERROR`

## 5. 토큰 / 보안 설계

- **Access-Token**: JWT(HS256), 60분, 클레임 `typ=access`, `deviceId`, `userType`. 앱은 `Authorization: Bearer` 로 사용
- **challengeToken**: 서명된 JWT(5분), `typ=mfa`, `jti` + 기기정보 클레임. access 와 상호 대체 불가
  - 일회용/최신 1개만 유효: `user_info.mfa_jti` 에 현재 jti 저장, 새 login 이 덮어쓰고 OTP 성공 시 비움
  - 그래서 별도 `login_challenge` 테이블은 없음
- **SMS/카카오 OTP**: 6자리, 유효 180초, 재발송 60초 간격. 평문 저장 금지, `HMAC-SHA256(userId:code)` 해시만 `user_info` 에 저장.
  사용자당 유효 OTP 1개(새 발송이 이전 코드를 덮어씀), 소진은 조건부 UPDATE 로 1회용 보장, 비교는 상수 시간
- **TOTP**: RFC 6238(HMAC-SHA1, 30초, 6자리, ±1 step). 시크릿은 기존 `user.totp_secret`, 같은 step 재사용 방지용 `user_info.last_totp_step`
- 실패 카운트 증가가 롤백되면 안 되므로 `AuthService` 에는 의도적으로 `@Transactional` 을 쓰지 않음
- **개발용 고정 OTP**: `otp.dev-fixed-code: "000000"`(application.yaml). 값이 있으면 loginType=1 은 발송 코드가 항상 000000(해시 저장/검증 흐름은 그대로),
  loginType=0 은 000000 이 TOTP 계산·등록 여부와 무관하게 통과. **운영 전 이 설정 삭제 필수**
- 로컬 개발 한정: 비밀번호 평문 비교, OTP 는 서버 콘솔(`[DEV OTP]`)에 출력. **운영 전 해시 전환/실제 발송 구현체 교체/비밀키 교체 필수**

## 6. DB

- `user` 테이블은 **회사 운영 중이라 수정 불가**. 사용 컬럼: `user_id`, `password`, `login_type`, `user_type`, `password_fail_count`, `phone_number`, `totp_secret`
  (`user` 는 MySQL 예약어라 매핑 시 백틱 사용)
- 새로 만드는 테이블은 `user_info` **하나**. 파일: `src/main/resources/db/ddl_user_info.sql` (자동 실행 안 됨, 수동 실행)
  - 알림 설정: `push_enabled`, `push_notice_enabled`, `push_transaction_enabled`, `push_settlement_enabled` (locale/time_zone 은 미사용이라 제거)
    (보안 알림은 필수라 컬럼 없이 항상 발송. 광고성/야간 수신동의는 사용자 지시로 제거)
  - 등록 기기(1 사용자 = 1 기기): `device_id`(UNIQUE), `device_platform`, `os_version`, `device_model`, `app_version`, `expo_push_token`(UNIQUE), `expo_push_token_updated_at` (`android_id`·`device_registered_at` 는 미사용/개인정보 사유로 제거, 앱이 `androidId` 를 보내도 무시됨)
    (OS 알림 권한 컬럼 `notifications_enabled` 는 토큰 유무로 판단 가능해 제거. 토큰 갱신 시각은 장기 미갱신 토큰 정리용)
  - 인증 상태: `last_login_at`, `last_totp_step`, `mfa_jti`, `otp_code_hash`, `otp_channel`, `otp_expires_at`, `otp_sent_at`
  - 여러 기기를 동시에 허용하게 되면 기기 컬럼을 별도 테이블로 분리해야 함
- 푸시 종류 분류(가맹점 관리자 앱 기준으로 제안): 공지사항 / 거래내역(승인·취소) / 정산 / 보안(필수).
  Android 알림 채널 ID 를 `notice`, `transaction`, `settlement`, `security` 로 맞추면 서버 분류와 일치.
  종류가 5개 이상으로 늘면 컬럼 대신 `user_id + category` 행 테이블 권장
- 잠금 해제(관리자): `update user set password_fail_count = 0 where user_id = '...'`

## 7. 결정 이력 (요약)

1. MyBatis-Plus 연동 + properties → YAML, 로컬 DB(root, 비밀번호 없음)
2. 로그인 컨트롤러 작성, `app` 세션에 계약 전달
3. 평문 비교(로컬용), Kotlin/Java 툴체인 충돌 해결(Kotlin 2.2.21 + JDK 24 로 되돌림 → 이후 Java 로 새 프로젝트 이전)
4. 2단계 인증(OTP) 도입: 처음엔 매 요청 id/pw 동반 → challengeToken 으로 교체
5. 잠금 정책: OTP 임시 잠금 폐지, `password_fail_count` 합산 영구 잠금으로 통일
6. 응답 규격: 실제 성공만 200, 공통 봉투, MFA 단계는 202
7. 기기 등록: 등록 기기는 OTP 생략, 기기 1:1 덮어쓰기, `user_device` 를 `user_info` 에 통합
8. `login_challenge`, `otp_challenge` 테이블 제거(JWT + `user_info` 컬럼으로 대체)
9. 아이디 존재 여부 노출 방지: 로그인 오류 응답에 남은 횟수 미포함(OTP 단계에서만 `remainingAttempts`)

## 8. 남은 일 / 미결

- DB 에 `user_info` 생성(위 DDL 실행), 서버 실행(`./gradlew bootRun`)으로 실제 로그인 확인 — **아직 한 번도 실서버로 검증하지 않음**
  - 접속 확인 필요: 터미널 `mysql -uroot` 는 비밀번호 없이 접속 거부됐었음
- 테스트 계정 준비: `login_type=1` 계정(`phone_number`), `login_type=0` 계정(`totp_secret` Base32) → app 세션이 실서버로 재검증 예정
- 영구 잠금의 "타인이 5번 틀리면 잠김" 부작용 수용 여부 사용자 확인 대기
- TOTP 등록 API 없음(`TOTP_NOT_ENROLLED` 시 앱은 관리자 문의 안내)
- 푸시 설정(공지/거래/정산) 변경 API 없음, 앱의 `PUT /api/devices/me` 호출은 expo-notifications/EAS projectId 준비 후 추가 예정
- SMS/카카오 실발송 구현체(`OtpSender`) 교체, 비밀번호 해시 전환, 시크릿 키 교체, 만료 데이터 정리 필요 시 배치
- 기본 테스트 `DemoApplicationTests`(`contextLoads`)는 DB 접속이 필요함

## 9. 실행 방법

```bash
# 1) DB: 스키마 app 준비 후 src/main/resources/db/ddl_user_info.sql 실행
# 2) 서버 실행 (기본 포트 8080)
./gradlew bootRun
```
