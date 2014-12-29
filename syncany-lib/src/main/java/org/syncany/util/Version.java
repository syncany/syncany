/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;
import com.google.common.base.Objects;
import com.google.common.primitives.Ints;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public class Version implements Comparable<Version> {

	public enum Release {
		PRE_ALPHA, ALPHA, PRE_BETA, BETA, FINAL
	}

	public static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)((\\.(\\d))?(-((pre-)?(alpha|beta+)?))?(\\+(.+)?)?)", Pattern.CASE_INSENSITIVE);
	public static final int GROUP_MAJOR = 1;
	public static final int GROUP_MINOR = 2;
	public static final int GROUP_PATCH = 5;
	public static final int GROUP_RELEASE = 7;
	public static final int GROUP_SPECIAL = 11;

	private final int major;
	private final int minor;
	private final int patch;
	private final Release release;
	private final String special;

	public static Version parse(String version) throws IllegalArgumentException {
		Matcher matcher = VERSION_PATTERN.matcher(version);

		if (matcher.matches()) {
			int major = Ints.tryParse(matcher.group(GROUP_MAJOR));
			int minor = Ints.tryParse(matcher.group(GROUP_MINOR));
			int patch = matcher.group(GROUP_PATCH) != null ? Ints.tryParse(matcher.group(GROUP_PATCH)) : 0;
			Release release;
			String special = matcher.group(GROUP_SPECIAL);

			String releaseStr = matcher.group(GROUP_RELEASE);

			if(releaseStr != null) {
				release = Release.valueOf(releaseStr.replace("-", "_").toUpperCase());
			}
			else {
				release = Release.FINAL;
			}

			return new Version(major, minor, patch, release, special);
		}
		else {
			throw new IllegalArgumentException("Invalid version format");
		}
	}

	public Version(int major, int minor) {
		this(major, minor, 0, null, null);
	}

	public Version(int major, int minor, int patch) {
		this(major, minor, patch, null, null);
	}

	public Version(int major, int minor, int patch, Release release) {
		this(major, minor, patch, release, null);
	}

	public Version(int major, int minor, int patch, Release release, String special) {
		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.special = special;

		if (release == null) {
			this.release = Release.FINAL;
		}
		else {
			this.release = release;
		}
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getPatch() {
		return patch;
	}

	public Release getRelease() {
		return release;
	}

	public String getSpecial() {
		return special;
	}

	public boolean isNewerThan(Version other) {
		return this.compareTo(other) == 1;
	}

	public boolean isOlderThan(Version other) {
		return this.compareTo(other) == -1;
	}

	public boolean isTheSameAs(Version other) {
		return this.compareTo(other) == 0;
	}

	public boolean isNewerOrEqualThan(Version other) {
		return isNewerThan(other) || isTheSameAs(other);
	}

	public boolean isOlderOrEqualThan(Version other) {
		return isOlderThan(other) || isTheSameAs(other);
	}


	@Override
	public int compareTo(Version that) {
		if (that == null) {
			return 1;
		}

		// shorten the process
		if (this.toString().equals(that.toString())) {
			return 0;
		}

		// compare major
		if (this.major > that.major) {
			return 1;
		}
		else if (this.major < that.major) {
			return -1;
		}

		// compare minor
		if (this.minor > that.minor) {
			return 1;
		}
		else if (this.minor < that.minor) {
			return -1;
		}

		// compare patch
		if (this.patch > that.patch) {
			return 1;
		}
		else if (this.patch < that.patch) {
			return -1;
		}

		// release
		if (this.release.ordinal() > that.release.ordinal()) {
			return 1;
		}
		else if (this.release.ordinal() < that.release.ordinal()) {
			return -1;
		}

		// special
		if (!Objects.equal(this.special, that.special)) {
			return 1;
		}

		// catchall
		return 0;

	}

	@Override
	public boolean equals(Object that) {
		return this == that || that != null && this.getClass() == that.getClass() && this.compareTo((Version) that) == 0;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%d.%d.%d", major, minor, patch));

		if (getRelease() != Release.FINAL) {
			sb.append("-");
			sb.append(release.toString().replace("_", "-").toLowerCase());
		}

		if (getSpecial() != null) {
			sb.append("+");
			sb.append(special);
		}

		return sb.toString();
	}

	public static class VersionConverter implements Converter<Version> {

		public Version read(InputNode node) throws Exception {
			int major = Ints.tryParse(node.getAttribute("major").getValue());
			int minor = Ints.tryParse(node.getAttribute("minor").getValue());
			int patch = Ints.tryParse(node.getAttribute("patch").getValue());
			Release release = Release.values()[Ints.tryParse(node.getAttribute("release").getValue())];
			String special = node.getAttribute("special") != null ? node.getAttribute("special").getValue() : null;

			return new Version(major, minor, patch, release, special);
		}

		public void write(OutputNode node, Version version) {
			node.setAttribute("major", String.valueOf(version.getMajor()));
			node.setAttribute("minor", String.valueOf(version.getMinor()));
			node.setAttribute("patch", String.valueOf(version.getPatch()));
			node.setAttribute("release", String.valueOf(version.getRelease().ordinal()));

			if (version.getSpecial() != null) {
				node.setAttribute("special", String.valueOf(version.getPatch()));
			}
		}
	}

}
