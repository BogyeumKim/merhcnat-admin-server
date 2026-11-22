package com.app.demo.appstatus.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.app.demo.appstatus.domain.AppPlatform;
import com.app.demo.appstatus.domain.AppVersion;
import com.app.demo.appstatus.domain.VersionPolicy;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** app_setting 의 {PLATFORM}_MIN_VERSION 키를 플랫폼별 필수 버전으로 읽는다 (DB 없이 매퍼를 대신해 검증) */
class AppSettingVersionPolicyTest {

	private final AppMaintenanceMapper maintenanceMapper = mock(AppMaintenanceMapper.class);
	private final AppSettingMapper settingMapper = mock(AppSettingMapper.class);
	private final AppStatusRepositoryImpl repository = new AppStatusRepositoryImpl(maintenanceMapper, settingMapper);

	@Test
	@DisplayName("플랫폼별 필수 버전 키를 읽는다")
	void readsMinVersionPerPlatform() {
		givenSettings(setting("IOS_MIN_VERSION", "2.0.0"), setting("ANDROID_MIN_VERSION", "1.5.0"), setting("OTHER_KEY", "x"));

		assertThat(repository.findVersionPolicies()).containsExactlyInAnyOrder(
				new VersionPolicy(AppPlatform.IOS, AppVersion.parse("2.0.0").orElseThrow()),
				new VersionPolicy(AppPlatform.ANDROID, AppVersion.parse("1.5.0").orElseThrow()));
	}

	@Test
	@DisplayName("설정이 없거나 형식이 잘못된 플랫폼은 버전 제한 없음")
	void skipsMissingOrMalformed() {
		givenSettings(setting("IOS_MIN_VERSION", "v2"));

		assertThat(repository.findVersionPolicies()).isEmpty();
	}

	private void givenSettings(AppSettingEntity... settings) {
		when(settingMapper.selectList(any())).thenReturn(List.of(settings));
	}

	private static AppSettingEntity setting(String key, String value) {
		AppSettingEntity entity = new AppSettingEntity();
		entity.setSettingKey(key);
		entity.setSettingValue(value);
		return entity;
	}
}
