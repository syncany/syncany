package org.syncany.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.syncany.tests.app.SyncanyCommandLineTest;
import org.syncany.tests.chunk.FixedOffsetChunkerTest;
import org.syncany.tests.chunk.FrameworkCombinationTest;
import org.syncany.tests.chunk.MultiChunkerTest;
import org.syncany.tests.chunk.TTTDChunkerTest;
import org.syncany.tests.config.EncryptionTest;
import org.syncany.tests.connection.plugins.local.LocalConnectionPluginTest;
import org.syncany.tests.database.DatabaseXmlDAOTest;
import org.syncany.tests.database.VectorClockTest;
import org.syncany.tests.operations.IndexerTest;
import org.syncany.tests.operations.StatusOperationTest;
import org.syncany.tests.operations.SyncUpOperationTest;
import org.syncany.tests.util.FileUtilTests;

@RunWith(Suite.class)
@SuiteClasses({
	// App
	SyncanyCommandLineTest.class,
	
	// Util
	FileUtilTests.class,
	
	// Config	
	EncryptionTest.class,
	
	// Chunking Framework
	MultiChunkerTest.class,
	FixedOffsetChunkerTest.class,
	TTTDChunkerTest.class,
	FrameworkCombinationTest.class,
	
	// Connection
	LocalConnectionPluginTest.class,
	
	// Database
	DatabaseXmlDAOTest.class,
	VectorClockTest.class,
	
	// Operations
	IndexerTest.class,
	SyncUpOperationTest.class,
	StatusOperationTest.class
})
public class OtherShortTests {
	// This class executes all tests
}
