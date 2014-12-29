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
package org.syncany.tests.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.syncany.util.Version;

public class VersionTest {


	private final Version v023 = Version.parse("0.2.3");
	private final Version v123 = Version.parse("1.2.3");
	private final Version v024 = Version.parse("0.2.4");
	private final Version v02 = Version.parse("0.2");
	private final Version v023a = Version.parse("0.2.2-alpha");
	private final Version v023pa = Version.parse("0.2.2-pre-alpha");
	private final Version v023pass = Version.parse("0.2.2-pre-alpha+SNAPSHOT");

	@Test
	public void testParsing() throws Exception {

		assertEquals(0, v023.getMajor());
		assertEquals(2, v023.getMinor());
		assertEquals(3, v023.getPatch());
		assertEquals(Version.Release.FINAL, v023.getRelease());
		assertNull(v023.getSpecial());

		assertEquals(0, v02.getMajor());
		assertEquals(2, v02.getMinor());
		assertEquals(0, v02.getPatch());

		assertEquals(Version.Release.ALPHA, v023a.getRelease());
		assertEquals(Version.Release.PRE_ALPHA, v023pa.getRelease());

		assertEquals(Version.Release.PRE_ALPHA, v023pass.getRelease());
		assertEquals("SNAPSHOT", v023pass.getSpecial());
	}

	@Test
	public void testCompare() throws Exception {
		assertEquals(-1, v023.compareTo(v123));
		assertEquals(1, v123.compareTo(v023));
		assertEquals(0, v123.compareTo(v123));

		assertEquals(-1, v023.compareTo(v024));
		assertEquals(1, v024.compareTo(v023));
		assertEquals(0, v024.compareTo(v024));

		assertEquals(0, v023pa.compareTo(v023pa));
		assertEquals(-1, v023pa.compareTo(v023a));
		assertEquals(1, v023a.compareTo(v023pa));

		assertEquals(1, v023pa.compareTo(v023pass));
	}

	@Test
	public void testToString() throws Exception {
		assertEquals("0.2.3", v023.toString());
		assertEquals("0.2.2-alpha", v023a.toString());
		assertEquals("0.2.2-pre-alpha", v023pa.toString());
		assertEquals("0.2.2-pre-alpha+SNAPSHOT", v023pass.toString());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalid() throws Exception {
		Version.parse("022");
		Version.parse("aa");
	}

}
