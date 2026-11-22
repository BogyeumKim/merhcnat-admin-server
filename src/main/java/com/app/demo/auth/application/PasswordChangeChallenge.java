package com.app.demo.auth.application;

/** 임시비밀번호 변경 안내: 이후 변경 API 에 쓰는 일회용 챌린지 토큰 */
public record PasswordChangeChallenge(String userId, String challengeToken) {
}
