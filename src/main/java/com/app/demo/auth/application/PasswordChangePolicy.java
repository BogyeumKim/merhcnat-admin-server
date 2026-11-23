package com.app.demo.auth.application;

import com.app.demo.auth.config.AuthProperties;
import com.app.demo.user.domain.PasswordChangeReason;
import com.app.demo.user.domain.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 로그인을 완료하기 전에 새 비밀번호 등록을 강제할지와 그 이유(임시비밀번호 / 만료)를 판단한다. */
@Component
@RequiredArgsConstructor
class PasswordChangePolicy {

	private final AuthProperties authProps;
	private final Clock clock;

	/** 변경이 필요하면 그 이유, 아니면 empty */
	Optional<PasswordChangeReason> reasonFor(User user) {
		return user.passwordChangeReason(LocalDateTime.now(clock), authProps.getPasswordExpireDays());
	}

	boolean isRequired(User user) {
		return reasonFor(user).isPresent();
	}
}
