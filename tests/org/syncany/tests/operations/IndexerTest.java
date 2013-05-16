package org.syncany.tests.operations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.syncany.chunk.Chunk;
import org.syncany.chunk.Chunker;
import org.syncany.chunk.CustomMultiChunker;
import org.syncany.chunk.Deduper;
import org.syncany.chunk.DeduperListener;
import org.syncany.chunk.FixedOffsetChunker;
import org.syncany.chunk.GzipCompressor;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.MultiChunker;
import org.syncany.chunk.Transformer;
import org.syncany.config.Constants;
import org.syncany.database.ChunkEntry;
import org.syncany.database.Database;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.operations.Indexer;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.util.FileLister;
import org.syncany.util.FileLister.FileListerListener;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class IndexerTest {
	private File tempLocalSourceDir;
	private File tempLocalCacheDir;
	
	@Before
	public void setUp() throws Exception {
		File rootDir = TestFileUtil.createTempDirectoryInSystemTemp();
		
		tempLocalSourceDir = new File(rootDir+"/local");
		tempLocalSourceDir.mkdir();
		
		tempLocalCacheDir = new File(rootDir+"/cache");		
		tempLocalCacheDir.mkdir();
	}
	
	@After
	public void tearDown() {
		TestFileUtil.deleteDirectory(tempLocalSourceDir);
	}
	
	@Test
	@Ignore
	public void testDeduperWithDatabase() throws IOException {
		// TODO write indexer test
	}

}
