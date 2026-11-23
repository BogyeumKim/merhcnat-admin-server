package com.app.demo.user.infrastructure.persistence;

import com.app.demo.global.crypto.KmsCipher;
import com.app.demo.user.domain.PasswordHash;
import com.app.demo.user.domain.UseType;
import com.app.demo.user.domain.User;
import com.app.demo.user.domain.UserRepository;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** {@link UserRepository} 의 MyBatis-Plus 구현. 엔티티 → 도메인 변환을 담당한다. */
@Repository
@RequiredArgsConstructor
class UserRepositoryImpl implements UserRepository {

	private final UserMapper mapper;
	private final KmsCipher cipher;

	@Override
	public Optional<User> findById(String userId) {
		UserEntity e = mapper.selectById(userId);
		if (e == null) {
			return Optional.empty();
		}
		int failCount = e.getPasswordFailCount() == null ? 0 : e.getPasswordFailCount();
		boolean tempPassword = e.getTmpPassword() != null && e.getTmpPassword() == 1;
		User user = User.with()
				.userId(e.getUserId())
				.password(e.getPassword())
				.loginTypeCode(e.getLoginType())
				.userType(e.getUserType())
				.passwordFailCount(failCount)
				.phoneNumber(cipher.decrypt(e.getPhoneNumberEnc()))
				.email(cipher.decrypt(e.getEmailEnc()))
				.totpSecret(e.getTotpSecret())
				.lastChangePw(e.getLastChangePw())
				.oldPasswords(oldPasswords(e))
				.useType(UseType.fromCode(e.getUseType()))
				.tempPassword(tempPassword)
				.create();
		return Optional.of(user);
	}

	@Override
	public boolean reserveAttempt(String userId, int max) {
		// 컬럼이 NULL 허용이라 COALESCE. 조건과 증가가 UPDATE 한 문장이라 원자적이다.
		return mapper.update(null, new UpdateWrapper<UserEntity>().eq("user_id", userId)
				.apply("COALESCE(password_fail_count, 0) < {0}", max)
				.setSql("password_fail_count = COALESCE(password_fail_count, 0) + 1")) == 1;
	}

	@Override
	public void refundAttempt(String userId) {
		mapper.update(null, new UpdateWrapper<UserEntity>().eq("user_id", userId)
				.gt("password_fail_count", 0)
				.setSql("password_fail_count = password_fail_count - 1"));
	}

	@Override
	public int failCount(String userId) {
		UserEntity e = mapper.selectById(userId);
		return e == null || e.getPasswordFailCount() == null ? 0 : e.getPasswordFailCount();
	}

	@Override
	public void resetFailCount(String userId) {
		mapper.update(null, new UpdateWrapper<UserEntity>().eq("user_id", userId).set("password_fail_count", 0));
	}

	@Override
	public void changePassword(String userId, String newPassword, LocalDateTime now) {
		String newPasswordHash = PasswordHash.of(newPassword);
		// MySQL 은 SET 을 왼쪽부터 순서대로 평가하므로 old3 ← old2 ← old1 ← 현재 ← 새 값 순으로 밀어야 값이 겹쳐 쓰이지 않는다.
		mapper.update(null, new UpdateWrapper<UserEntity>().eq("user_id", userId)
				.setSql("old_password3 = old_password2, old_password2 = old_password1, old_password1 = password")
				.set("password", newPasswordHash).set("last_change_pw", now).set("tmp_password", 0));
	}

	/** 비어 있지 않은 이전 비밀번호만 최신순으로 모은다 */
	private static List<String> oldPasswords(UserEntity e) {
		List<String> list = new ArrayList<>(3);
		for (String p : new String[] {e.getOldPassword1(), e.getOldPassword2(), e.getOldPassword3()}) {
			if (p != null && !p.isEmpty()) {
				list.add(p);
			}
		}
		return list;
	}
}
