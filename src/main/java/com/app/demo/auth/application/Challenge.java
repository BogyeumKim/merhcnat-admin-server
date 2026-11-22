package com.app.demo.auth.application;

import com.app.demo.auth.domain.MfaClaims;
import com.app.demo.user.domain.User;
import com.app.demo.user.domain.UserInfo;

/** 검증을 마친 챌린지 토큰 컨텍스트: 토큰 클레임 + 최신 사용자/부가정보 */
record Challenge(MfaClaims claims, User user, UserInfo info) {
}
