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
import org.syncany.tests.integration.cli.CliPluginsTest;
import org.syncany.tests.integration.cli.CommandLineInterfaceTest;
import org.syncany.tests.integration.cli.ConnectCommandWithEncryptionTest;
import org.syncany.tests.integration.cli.DownCommandTest;
import org.syncany.tests.integration.cli.GenlinkCommandTest;
import org.syncany.tests.integration.cli.InitAndConnectCommandNoEncryptionTest;
import org.syncany.tests.integration.cli.LsCommandTest;
import org.syncany.tests.integration.cli.LsRemoteCommandTest;
import org.syncany.tests.integration.cli.PluginCommandTest;
import org.syncany.tests.integration.cli.RestoreCommandTest;
import org.syncany.tests.integration.cli.StatusCommandTest;
import org.syncany.tests.integration.cli.UpCommandTest;
import org.syncany.tests.integration.cli.WatchCommandTest;

@RunWith(Suite.class)
@SuiteClasses({
	CliPluginsTest.class,
	CommandLineInterfaceTest.class,
	ConnectCommandWithEncryptionTest.class,
	DownCommandTest.class,
	GenlinkCommandTest.class,
	InitAndConnectCommandNoEncryptionTest.class,
	LsCommandTest.class,
	LsRemoteCommandTest.class,
	PluginCommandTest.class,
	RestoreCommandTest.class,
	StatusCommandTest.class,
	UpCommandTest.class,	
	WatchCommandTest.class
})
public class CliTestSuite {
	// This class executes all tests
}
