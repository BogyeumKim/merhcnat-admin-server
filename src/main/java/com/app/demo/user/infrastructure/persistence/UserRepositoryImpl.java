package com.app.demo.user.infrastructure.persistence;

import com.app.demo.user.domain.User;
import com.app.demo.user.domain.UserRepository;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** {@link UserRepository} 의 MyBatis-Plus 구현. 엔티티 → 도메인 변환을 담당한다. */
@Repository
class UserRepositoryImpl implements UserRepository {

	private final UserMapper mapper;

	UserRepositoryImpl(UserMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public Optional<User> findById(String userId) {
		UserEntity e = mapper.selectById(userId);
		if (e == null) {
			return Optional.empty();
		}
		int failCount = e.getPasswordFailCount() == null ? 0 : e.getPasswordFailCount();
		return Optional.of(new User(e.getUserId(), e.getPassword(), e.getLoginType(), e.getUserType(),
				failCount, e.getPhoneNumber(), e.getTotpSecret()));
	}

	@Override
	public int increaseFailCount(String userId) {
		// 컬럼이 NULL 허용이라 COALESCE, 증가는 DB 에서 원자적으로 수행
		mapper.update(null, new UpdateWrapper<UserEntity>().eq("user_id", userId)
				.setSql("password_fail_count = COALESCE(password_fail_count, 0) + 1"));
		UserEntity e = mapper.selectById(userId);
		return e == null || e.getPasswordFailCount() == null ? 0 : e.getPasswordFailCount();
	}

	@Override
	public void resetFailCount(String userId) {
		mapper.update(null, new UpdateWrapper<UserEntity>().eq("user_id", userId).set("password_fail_count", 0));
	}
}
