package org.syncany.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.syncany.tests.chunk.MultiChunkerTest;
import org.syncany.tests.chunk.FixedOffsetChunkerTest;
import org.syncany.tests.chunk.FrameworkCombinationTest;
import org.syncany.tests.chunk.TTTDChunkerTest;
import org.syncany.tests.config.EncryptionTest;
import org.syncany.tests.connection.plugins.local.LocalConnectionPluginTest;
import org.syncany.tests.database.DatabaseXmlDAOTest;
import org.syncany.tests.database.VectorClockTest;
import org.syncany.tests.operations.IndexerTest;
import org.syncany.tests.operations.SyncUpOperationTest;
import org.syncany.tests.scenarios.Scenario2Test;
import org.syncany.tests.scenarios.Scenario1Test;
import org.syncany.tests.scenarios.NoConflictsScenarioTest;
import org.syncany.tests.scenarios.Scenario3Test;
import org.syncany.tests.scenarios.Scenario4Test;

@RunWith(Suite.class)
@SuiteClasses({
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
	
	// Scenarios
	Scenario1Test.class,
	Scenario2Test.class,
	Scenario3Test.class,
	Scenario4Test.class,
	NoConflictsScenarioTest.class
})
public class AllTestsSuite {
	// This class executes all tests
}
