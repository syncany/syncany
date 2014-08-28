/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
import org.syncany.tests.operations.AssemblerTest;
import org.syncany.tests.operations.CleanupOperationTest;
import org.syncany.tests.operations.ConnectOperationTest;
import org.syncany.tests.operations.FileSystemActionComparatorTest;
import org.syncany.tests.operations.FileSystemActionReconciliatorTest;
import org.syncany.tests.operations.IndexerTest;
import org.syncany.tests.operations.InitOperationTest;
import org.syncany.tests.operations.NotificationListenerTest;
import org.syncany.tests.operations.OperationPerformanceTest;
import org.syncany.tests.operations.PluginOperationTest;
import org.syncany.tests.operations.RecursiveWatcherTest;
import org.syncany.tests.operations.StatusOperationTest;
import org.syncany.tests.operations.SyncUpOperationTest;

@RunWith(Suite.class)
@SuiteClasses({
	AssemblerTest.class,
	CleanupOperationTest.class,
	ConnectOperationTest.class,
	FileSystemActionComparatorTest.class,
	FileSystemActionReconciliatorTest.class,
	IndexerTest.class,
	InitOperationTest.class,
	NotificationListenerTest.class,
	OperationPerformanceTest.class,
	PluginOperationTest.class,
	RecursiveWatcherTest.class,
	StatusOperationTest.class,
	SyncUpOperationTest.class,
})
public class OperationTestSuite {
	// This class executes all tests
}
