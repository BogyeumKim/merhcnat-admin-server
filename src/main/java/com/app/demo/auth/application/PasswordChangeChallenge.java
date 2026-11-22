package com.app.demo.auth.application;

import com.app.demo.user.domain.PasswordChangeReason;

/**
 * 비밀번호 변경 안내: 변경이 필요한 이유와 이후 변경 API({@code /api/auth/password/temp})에 쓰는 일회용 챌린지 토큰.
 *
 * @param reason 임시비밀번호(TEMP_PASSWORD) / 비밀번호 만료(PASSWORD_EXPIRED)
 */
public record PasswordChangeChallenge(String userId, String challengeToken, PasswordChangeReason reason) {
}
