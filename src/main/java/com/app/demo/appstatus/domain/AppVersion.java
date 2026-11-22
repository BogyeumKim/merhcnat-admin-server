package com.app.demo.appstatus.domain;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 앱 버전(semver: major.minor.patch). 숫자 단위로 비교한다(1.10.0 &gt; 1.9.0).
 * 뒤에 붙는 pre-release/build 표기(-beta.1, +45)는 비교에서 무시한다: 스토어 배포 버전은 세 자리 숫자로만 판단한다.
 */
public record AppVersion(int major, int minor, int patch) implements Comparable<AppVersion> {

	private static final Pattern SEMVER = Pattern.compile("^(\\d{1,5})\\.(\\d{1,5})\\.(\\d{1,5})(?:[-+][0-9A-Za-z.+-]{1,30})?$");

	private static final Comparator<AppVersion> ORDER =
			Comparator.comparingInt(AppVersion::major).thenComparingInt(AppVersion::minor).thenComparingInt(AppVersion::patch);

	/** "1.2.3" 형식이 아니면 empty */
	public static Optional<AppVersion> parse(String value) {
		Matcher m = SEMVER.matcher(value.strip());
		if (!m.matches()) {
			return Optional.empty();
		}
		return Optional.of(new AppVersion(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))));
	}

	public boolean isLowerThan(AppVersion other) {
		return compareTo(other) < 0;
	}

	@Override
	public int compareTo(AppVersion other) {
		return ORDER.compare(this, other);
	}

	@Override
	public String toString() {
		return major + "." + minor + "." + patch;
	}
}
