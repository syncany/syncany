package org.syncany.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.syncany.tests.chunk.ChunkerTest;
import org.syncany.tests.communication.CommunicationSocketTest;
import org.syncany.tests.config.EncryptionTest;
import org.syncany.tests.connection.UploadTest;
import org.syncany.tests.db.ChunkCacheFileTest;
import org.syncany.tests.db.CloneFileTreeTest;
import org.syncany.tests.db.ReadDBTest;
import org.syncany.tests.index.IndexerTest;
import org.syncany.tests.watcher.local.BufferedWatcherTest;
import org.syncany.tests.watcher.local.LocalWatcherTest;

@RunWith(Suite.class)
@SuiteClasses({
	// Type in here all tests (execution in order)
	ChunkerTest.class,
	EncryptionTest.class,
	UploadTest.class,
	ChunkCacheFileTest.class,
	CloneFileTreeTest.class,
	ReadDBTest.class,
	IndexerTest.class,
	CommunicationSocketTest.class,
	LocalWatcherTest.class,
	BufferedWatcherTest.class
})
public class AllTests {

	// this class executes all tests
}
