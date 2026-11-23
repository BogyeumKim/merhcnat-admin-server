package com.app.demo.auth.application;

import com.app.demo.auth.domain.MfaClaims;
import com.app.demo.user.domain.User;
import com.app.demo.user.domain.UserInfo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 검증을 마친 챌린지 토큰 컨텍스트: 토큰 클레임 + 최신 사용자/부가정보 */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class Challenge {

	private final MfaClaims claims;

	private final User user;

	private final UserInfo info;
}
