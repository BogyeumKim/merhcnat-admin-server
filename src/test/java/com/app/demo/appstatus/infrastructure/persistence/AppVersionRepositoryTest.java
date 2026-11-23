package com.app.demo.appstatus.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.app.demo.appstatus.domain.AppPlatform;
import com.app.demo.appstatus.domain.AppStatusRepository;
import com.app.demo.appstatus.domain.AppVersion;
import com.app.demo.appstatus.domain.VersionRelease;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 필수 버전 조회: use_flg = 1 인 행 중 플랫폼별 "지금 적용 중 1건 + 예약된 것 전부".
 * 실행 중인 서버의 현재 필수 버전에 영향을 주지 않도록 2099년 이후 행만 넣고, 넣은 행만 지운다.
 */
@SpringBootTest
class AppVersionRepositoryTest {

	/** 이 시각을 "지금"으로 보고 조회한다 (실제 운영 행은 모두 이보다 과거) */
	private static final LocalDateTime NOW = LocalDateTime.of(2099, 6, 1, 0, 0);

	@Autowired
	AppStatusRepository repository;

	@Autowired
	AppVersionMapper mapper;

	private final List<Long> insertedIds = new ArrayList<>();

	@AfterEach
	void cleanUp() {
		insertedIds.forEach(mapper::deleteById);
	}

	@Test
	@DisplayName("지금 적용 중인 1건(가장 최근)과 예약된 것만 돌려주고, 그 이전 이력은 제외한다")
	void currentPlusScheduled() {
		insert("IOS", "9.0.0", NOW.minusMonths(2), 1);
		insert("IOS", "9.1.0", NOW.minusMonths(1), 1);
		insert("IOS", "9.2.0", NOW.plusMonths(1), 1);

		assertThat(iosVersions()).containsExactly("9.1.0", "9.2.0");
	}

	@Test
	@DisplayName("use_flg = 0(미사용, 기본값) 행은 조회되지 않는다")
	void excludesDisabled() {
		insert("IOS", "9.5.0", NOW.minusMonths(1), 1);
		insert("IOS", "9.6.0", NOW.minusDays(1), null); // 기본값 0
		insert("IOS", "9.7.0", NOW.plusDays(1), 0);

		assertThat(iosVersions()).containsExactly("9.5.0");
	}

	@Test
	@DisplayName("버전 형식이 잘못된 예약 행은 건너뛴다")
	void skipsMalformed() {
		insert("IOS", "9.8.0", NOW.minusDays(1), 1);
		insert("IOS", "v10", NOW.plusDays(1), 1);

		assertThat(iosVersions()).containsExactly("9.8.0");
	}

	/** 이 테스트가 넣은 범위(2099년)의 iOS 버전만 본다 */
	private List<String> iosVersions() {
		return repository.findEnabledVersionReleasesFrom(NOW).stream()
				.filter(r -> r.getPlatform() == AppPlatform.IOS && r.getForceAt().getYear() >= 2099)
				.map(VersionRelease::getVersion).map(AppVersion::asText).toList();
	}

	private void insert(String platform, String version, LocalDateTime forceAt, Integer useFlg) {
		AppVersionEntity entity = new AppVersionEntity();
		entity.setPlatform(platform);
		entity.setVersion(version);
		entity.setForceDt(forceAt);
		entity.setUseFlg(useFlg); // null 이면 insert 에서 빠져 DB 기본값(0)이 적용된다
		mapper.insert(entity);
		insertedIds.add(entity.getId());
	}
}
