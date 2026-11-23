package com.app.demo.auth.application;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TempPasswordChangeCommand {

	private final String challengeToken;

	private final String currentPassword;

	private final String newPassword;
}
