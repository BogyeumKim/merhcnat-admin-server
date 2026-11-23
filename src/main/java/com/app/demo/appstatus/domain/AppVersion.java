package com.app.demo.appstatus.domain;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 앱 버전(semver: major.minor.patch). 숫자 단위로 비교한다(1.10.0 &gt; 1.9.0).
 * 뒤에 붙는 pre-release/build 표기(-beta.1, +45)는 비교에서 무시한다: 스토어 배포 버전은 세 자리 숫자로만 판단한다.
 */
@Getter
@EqualsAndHashCode
@Builder(builderMethodName = "with", buildMethodName = "create")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AppVersion implements Comparable<AppVersion> {

	private static final Pattern SEMVER = Pattern.compile("^(\\d{1,5})\\.(\\d{1,5})\\.(\\d{1,5})(?:[-+][0-9A-Za-z.+-]{1,30})?$");

	private static final Comparator<AppVersion> ORDER =
			Comparator.comparingInt(AppVersion::getMajor).thenComparingInt(AppVersion::getMinor).thenComparingInt(AppVersion::getPatch);

	private final int major;

	private final int minor;

	private final int patch;

	/** "1.2.3" 형식이 아니면 empty */
	public static Optional<AppVersion> parse(String value) {
		Matcher m = SEMVER.matcher(value.strip());
		if (!m.matches()) {
			return Optional.empty();
		}
		int major = Integer.parseInt(m.group(1));
		int minor = Integer.parseInt(m.group(2));
		int patch = Integer.parseInt(m.group(3));
		return Optional.of(new AppVersion(major, minor, patch));
	}

	public boolean isLowerThan(AppVersion other) {
		return compareTo(other) < 0;
	}

	@Override
	public int compareTo(AppVersion other) {
		return ORDER.compare(this, other);
	}

	/** 세 자리 숫자 표기. ex) 1.2.3-beta.1 로 읽은 버전 → {@code "1.2.3"} */
	public String asText() {
		return major + "." + minor + "." + patch;
	}
}
