package org.syncany.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.syncany.tests.chunk.ChunkAndDBTest130128;
import org.syncany.tests.chunk.ChunkerTest;
import org.syncany.tests.chunk.SimpleChunkAndMultiChunkTest;
import org.syncany.tests.config.EncryptionTest;


@RunWith(Suite.class)
@SuiteClasses({
	// Type in here all tests (execution in order)
	ChunkerTest.class,
	EncryptionTest.class,
	//UploadTest.class,
	//IndexerTest.class,
	SimpleChunkAndMultiChunkTest.class,
	ChunkAndDBTest130128.class
})
public class AllTests {
	// This class executes all tests
}
