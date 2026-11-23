package com.app.demo.appstatus.application;

import com.app.demo.appstatus.domain.AppPlatform;
import com.app.demo.appstatus.domain.AppStatus;
import com.app.demo.appstatus.domain.AppVersion;
import com.app.demo.appstatus.domain.MaintenanceWindow;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * /api 요청의 입장 판단: 점검 기간인지 → 앱 버전이 필수 버전 이상인지.
 * <ol>
 *   <li>점검 대상 경로이고 지금이 점검 기간(fr_dt ≤ now &lt; to_dt)이면 버전과 관계없이 점검(503)이 우선한다.
 *       업데이트해도 점검 중에는 쓸 수 없기 때문이다.</li>
 *   <li>버전 헤더(X-App-Version, X-App-Platform)가 둘 다 없으면 통과한다(헤더를 보내지 않는 기존 클라이언트 호환).</li>
 *   <li>한쪽만 있거나, 알 수 없는 플랫폼이거나, 버전 형식이 틀리면 400 이다(조작되거나 깨진 요청).</li>
 *   <li>지금 적용 중인 필수 버전(app_version)이 없으면 통과하고, 그보다 낮으면 403 UPDATE_REQUIRED 다.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class AppAccessService {

	private final AppStatusSnapshot snapshot;
	private final Clock clock;

	public AccessDecision decide(boolean maintenanceApplies, @Nullable String platformHeader, @Nullable String versionHeader) {
		AppStatus status = snapshot.current();
		LocalDateTime now = LocalDateTime.now(clock);
		Optional<MaintenanceWindow> maintenance = maintenanceApplies
				? status.activeMaintenance(now)
				: Optional.empty();
		if (maintenance.isPresent()) {
			return AccessDecision.UnderMaintenance.of(maintenance.get());
		}
		if (platformHeader == null && versionHeader == null) {
			return AccessDecision.Allowed.of();
		}
		if (platformHeader == null || versionHeader == null) {
			return AccessDecision.InvalidClientHeaders.of();
		}
		Optional<AppPlatform> platform = AppPlatform.parse(platformHeader);
		Optional<AppVersion> version = AppVersion.parse(versionHeader);
		if (platform.isEmpty() || version.isEmpty()) {
			return AccessDecision.InvalidClientHeaders.of();
		}
		return status.requiredVersion(platform.get(), now)
				.filter(required -> version.get().isLowerThan(required.getVersion()))
				.<AccessDecision>map(AccessDecision.UpdateRequired::of)
				.orElseGet(AccessDecision.Allowed::of);
	}
}
