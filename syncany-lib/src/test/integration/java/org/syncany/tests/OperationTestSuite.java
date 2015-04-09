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
import org.syncany.tests.integration.operations.AssemblerTest;
import org.syncany.tests.integration.operations.CleanupOperationTest;
import org.syncany.tests.integration.operations.ConnectOperationTest;
import org.syncany.tests.integration.operations.FileSystemActionComparatorTest;
import org.syncany.tests.integration.operations.FileSystemActionReconciliatorTest;
import org.syncany.tests.integration.operations.IndexerTest;
import org.syncany.tests.integration.operations.InitOperationTest;
import org.syncany.tests.integration.operations.LogOperationTest;
import org.syncany.tests.integration.operations.NotificationListenerTest;
import org.syncany.tests.integration.operations.OperationPerformanceTest;
import org.syncany.tests.integration.operations.PluginOperationTest;
import org.syncany.tests.integration.operations.RecursiveWatcherTest;
import org.syncany.tests.integration.operations.SplitSyncUpOperationTest;
import org.syncany.tests.integration.operations.StatusOperationTest;
import org.syncany.tests.integration.operations.UpOperationTest;
import org.syncany.tests.integration.operations.UpdateOperationTest;
import org.syncany.tests.unit.operations.daemon.DaemonOperationTest;

@RunWith(Suite.class)
@SuiteClasses({
		AssemblerTest.class,
		CleanupOperationTest.class,
		ConnectOperationTest.class,
		DaemonOperationTest.class,
		FileSystemActionComparatorTest.class,
		FileSystemActionReconciliatorTest.class,
		IndexerTest.class,
		InitOperationTest.class,
		LogOperationTest.class,
		NotificationListenerTest.class,
		OperationPerformanceTest.class,
		PluginOperationTest.class,
		RecursiveWatcherTest.class,
		SplitSyncUpOperationTest.class,
		StatusOperationTest.class,
		UpOperationTest.class,
		UpdateOperationTest.class
})
public class OperationTestSuite {
	// This class executes all tests
}
