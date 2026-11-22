package com.app.demo.auth.application;

/** 로그인 완료 결과. accessToken 은 Access-Token 응답 헤더로 내려간다. */
public record LoginSuccess(SessionResult session, String accessToken) {
}
