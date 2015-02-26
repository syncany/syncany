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
package org.syncany.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.syncany.tests.unit.util.Base58Test;
import org.syncany.tests.unit.util.CollectionUtilTest;
import org.syncany.tests.unit.util.FileUtilTest;
import org.syncany.tests.unit.util.LimitedDosFileAttributesTest;
import org.syncany.tests.unit.util.LimitedSortedSetTest;
import org.syncany.tests.unit.util.NormalizedPathTest;
import org.syncany.tests.unit.util.PidFileUtilTest;
import org.syncany.tests.unit.util.ReflectionUtilTest;
import org.syncany.tests.unit.util.StringUtilTest;

@RunWith(Suite.class)
@SuiteClasses({
	Base58Test.class,
	CollectionUtilTest.class,
	FileUtilTest.class,
	LimitedSortedSetTest.class,
	LimitedDosFileAttributesTest.class,
	NormalizedPathTest.class,
	PidFileUtilTest.class,
	StringUtilTest.class,
	ReflectionUtilTest.class
})
public class UtilTestSuite {
	// This class executes all tests
}
