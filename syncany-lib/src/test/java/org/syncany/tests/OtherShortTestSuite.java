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
import org.syncany.tests.chunk.FixedOffsetChunkerTest;
import org.syncany.tests.chunk.FrameworkCombinationTest;
import org.syncany.tests.chunk.MultiChunkerTest;
import org.syncany.tests.chunk.TTTDChunkerTest;
import org.syncany.tests.config.ConfigHelperTest;
import org.syncany.tests.config.ConfigTest;
import org.syncany.tests.connection.plugins.PluginOptionSpecTest;
import org.syncany.tests.connection.plugins.PluginsTest;
import org.syncany.tests.connection.plugins.local.LocalTransferManagerPluginTest;
import org.syncany.tests.connection.plugins.unreliable_local.UploadInterruptedTest;
import org.syncany.tests.crypto.CipherSpecsTest;
import org.syncany.tests.crypto.CipherUtilTest;
import org.syncany.tests.crypto.MultiCipherStreamsTest;
import org.syncany.tests.operations.CleanupOperationTest;
import org.syncany.tests.operations.ConnectOperationTest;
import org.syncany.tests.operations.FileSystemActionComparatorTest;
import org.syncany.tests.operations.IndexerTest;
import org.syncany.tests.operations.InitOperationTest;
import org.syncany.tests.operations.NotificationListenerTest;
import org.syncany.tests.operations.OperationPerformanceTest;
import org.syncany.tests.operations.RecursiveWatcherTest;
import org.syncany.tests.operations.StatusOperationTest;
import org.syncany.tests.operations.SyncUpOperationTest;
import org.syncany.tests.util.FileUtilTest;

@RunWith(Suite.class)
@SuiteClasses({
	// Util
	FileUtilTest.class,
	
	// Crypto	
	CipherSpecsTest.class,
	CipherUtilTest.class,
	MultiCipherStreamsTest.class,
	
	// Chunking Framework
	MultiChunkerTest.class,
	FixedOffsetChunkerTest.class,
	TTTDChunkerTest.class,
	FrameworkCombinationTest.class,
	
	// Connection
	PluginsTest.class,
	PluginOptionSpecTest.class,
	LocalTransferManagerPluginTest.class,
	UploadInterruptedTest.class,
	
	// Operations
	FileSystemActionComparatorTest.class,
	IndexerTest.class,
	InitOperationTest.class,
	OperationPerformanceTest.class,
	RecursiveWatcherTest.class,
	SyncUpOperationTest.class,
	StatusOperationTest.class,
	NotificationListenerTest.class,
	ConnectOperationTest.class,
	CleanupOperationTest.class,
	
	// Config
	ConfigHelperTest.class,
	ConfigTest.class
})
public class OtherShortTestSuite {
	// This class executes all tests
}
