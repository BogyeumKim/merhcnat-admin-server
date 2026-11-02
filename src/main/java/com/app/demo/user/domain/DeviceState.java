package com.app.demo.user.domain;

/** 로그인 시도 기기의 등록 상태. */
public enum DeviceState {
	/** 이 사용자도 이 기기도 등록 정보 없음(최초) */
	NONE,
	/** 이 사용자에 등록된 기기와 같음 */
	MATCH,
	/** 이 사용자의 다른 기기가 등록돼 있거나, 이 기기가 다른 계정 소유 */
	CHANGED
}
