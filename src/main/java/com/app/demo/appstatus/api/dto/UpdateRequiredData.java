package com.app.demo.appstatus.api.dto;

import com.app.demo.global.api.ErrorDetail;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 403 UPDATE_REQUIRED 의 data: 업데이트해야 하는 필수 버전 */
@Getter
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UpdateRequiredData implements ErrorDetail {

	private final String minAppVersion;
}
