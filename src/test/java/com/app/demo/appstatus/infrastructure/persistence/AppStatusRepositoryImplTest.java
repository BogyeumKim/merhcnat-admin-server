package com.app.demo.appstatus.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.app.demo.appstatus.domain.AppStatusRepository;
import com.app.demo.appstatus.domain.MaintenanceWindow;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 점검 기간 조회는 use_flg = 1(사용) 이고 아직 끝나지 않은 행만 돌려준다. 테스트가 넣은 행은 끝나면 지운다. */
@SpringBootTest
class AppStatusRepositoryImplTest {

	/** 실제 운영 행과 겹치지 않도록 먼 미래 구간을 쓴다 */
	private static final LocalDateTime BASE = LocalDateTime.of(2099, 1, 1, 0, 0);

	@Autowired
	AppStatusRepository repository;

	@Autowired
	AppMaintenanceMapper mapper;

	private final List<Long> insertedIds = new ArrayList<>();

	@AfterEach
	void cleanUp() {
		insertedIds.forEach(mapper::deleteById);
	}

	@Test
	@DisplayName("use_flg = 0(미사용, 기본값) 행은 점검 기간으로 조회되지 않는다")
	void excludesDisabledWindows() {
		insert(BASE, BASE.plusHours(2), null); // 기본값 0
		insert(BASE.plusDays(1), BASE.plusDays(1).plusHours(2), 0);

		assertThat(windowsInTestRange()).isEmpty();
	}

	@Test
	@DisplayName("use_flg = 1(사용) 행만 점검 기간으로 조회된다")
	void includesOnlyEnabledWindows() {
		insert(BASE, BASE.plusHours(2), 0);
		insert(BASE.plusDays(1), BASE.plusDays(1).plusHours(2), 1);

		assertThat(windowsInTestRange())
				.containsExactly(MaintenanceWindow.of(BASE.plusDays(1), BASE.plusDays(1).plusHours(2)));
	}

	@Test
	@DisplayName("사용 중이어도 이미 끝난 기간은 조회되지 않는다")
	void excludesEndedWindows() {
		insert(BASE.minusHours(3), BASE.minusHours(1), 1);

		assertThat(windowsInTestRange()).isEmpty();
	}

	/** 테스트 구간(BASE 이후 시작) 행만 걸러 본다 */
	private List<MaintenanceWindow> windowsInTestRange() {
		return repository.findEnabledMaintenanceWindowsEndingAfter(BASE).stream()
				.filter(window -> !window.getFrom().isBefore(BASE.minusDays(1)))
				.toList();
	}

	private void insert(LocalDateTime from, LocalDateTime to, Integer useFlg) {
		AppMaintenanceEntity entity = new AppMaintenanceEntity();
		entity.setFrDt(from);
		entity.setToDt(to);
		entity.setUseFlg(useFlg); // null 이면 insert 에서 빠져 DB 기본값(0)이 적용된다
		mapper.insert(entity);
		insertedIds.add(entity.getId());
	}
}
