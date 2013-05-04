package org.syncany.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.syncany.tests.chunk.FixedOffsetChunkerTest;
import org.syncany.tests.chunk.FrameworkCombinationTests;
import org.syncany.tests.chunk.MultiChunkerTest;
import org.syncany.tests.chunk.TTTDChunkerTest;
import org.syncany.tests.config.EncryptionTest;
import org.syncany.tests.connection.local.LocalConnectionPluginTest;
import org.syncany.tests.connection.local.UploaderWithLocalPluginTest;


@RunWith(Suite.class)
@SuiteClasses({
	// Type in here all tests (execution in order)	
	EncryptionTest.class,
	
	// Chunking Framework
	MultiChunkerTest.class,
	FixedOffsetChunkerTest.class,
	TTTDChunkerTest.class,
	FrameworkCombinationTests.class,
	
	// Connection
	LocalConnectionPluginTest.class,
	UploaderWithLocalPluginTest.class
	
	// Alte Tests
	//ChunkAndDBTest130128.class
})
public class AllTests {
	// This class executes all tests
}
