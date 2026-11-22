package com.app.demo.auth.application;

public record TempPasswordChangeCommand(String challengeToken, String currentPassword, String newPassword) {
}
