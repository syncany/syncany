/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
import org.syncany.tests.cli.CommandLineInterfaceTest;
import org.syncany.tests.cli.DownCommandTest;
import org.syncany.tests.cli.LogCommandTest;
import org.syncany.tests.cli.LsRemoteCommandTest;
import org.syncany.tests.cli.StatusCommandTest;
import org.syncany.tests.cli.SyncCommandTest;
import org.syncany.tests.cli.UpCommandTest;
import org.syncany.tests.cli.WatchCommandTest;

@RunWith(Suite.class)
@SuiteClasses({
	CommandLineInterfaceTest.class,
	DownCommandTest.class,
	LogCommandTest.class,
	LsRemoteCommandTest.class,
	StatusCommandTest.class,
	SyncCommandTest.class,
	UpCommandTest.class,	
	WatchCommandTest.class
})
public class CliTests {
	// This class executes all tests
}
