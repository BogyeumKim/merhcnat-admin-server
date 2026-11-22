package com.app.demo.appstatus.api.dto;

/** 403 UPDATE_REQUIRED 의 data: 업데이트해야 하는 필수 버전 */
public record UpdateRequiredData(String minAppVersion) {
}
