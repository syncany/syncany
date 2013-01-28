package org.syncany.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.syncany.tests.chunk.ChunkAndDBTest130128;
import org.syncany.tests.chunk.ChunkerTest;
import org.syncany.tests.chunk.SimpleChunkAndMultiChunkTest;
import org.syncany.tests.config.EncryptionTest;
import org.syncany.tests.connection.UploadTest;
import org.syncany.tests.db.ChunkCacheFileTest;
import org.syncany.tests.db.CloneFileTreeTest;
import org.syncany.tests.db.ReadDBTest;
import org.syncany.tests.index.IndexerTest;

@RunWith(Suite.class)
@SuiteClasses({
	// Type in here all tests (execution in order)
	ChunkerTest.class,
	EncryptionTest.class,
	//UploadTest.class,
	ChunkCacheFileTest.class,
	CloneFileTreeTest.class,
	ReadDBTest.class,
	//IndexerTest.class,
	SimpleChunkAndMultiChunkTest.class,
	ChunkAndDBTest130128.class
})
public class AllTests {
	// This class executes all tests
}
