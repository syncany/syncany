package org.syncany.tests.db;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.syncany.db.ChunkEntry;
import org.syncany.db.FileContent;
import org.syncany.db.FileHistoryPart;
import org.syncany.db.FileVersion;
import org.syncany.db.MultiChunkEntry;
import org.syncany.tests.util.TestUtil;

public class DatabaseWriteReadRandomCombinationTest {
	private File tempDir;
	
	@Before
	public void setUp() throws Exception {
		tempDir = TestUtil.createTempDirectoryInSystemTemp();		
	}
	
	@After
	public void tearDown() {
		TestUtil.deleteDirectory(tempDir);
	}
	 
	@Test
	@Ignore
	public void testReadAndWriteWithRandomObjectsMultipleTimes() {
		int repeatCount = 3;
		
		for (int i=0; i<repeatCount; i++) {
			testReadAndWriteWithRandomObjects();
		}
	}
	
	private void testReadAndWriteWithRandomObjects() {
		List<ChunkEntry> chunks = createRandomChunks(500, 20);
		List<MultiChunkEntry> multiChunks = new ArrayList<MultiChunkEntry>();
		List<FileContent> fileContents = new ArrayList<FileContent>();
		List<FileHistoryPart> fileHistories = new ArrayList<FileHistoryPart>();
		List<FileVersion> fileVersions = new ArrayList<FileVersion>();
			
		// TODO
	}
	
	private List<ChunkEntry> createRandomChunks(int chunkCount, int checksumSize) {
		List<ChunkEntry> chunks = new ArrayList<ChunkEntry>();
		
		for (int i=0; i<chunkCount; i++) {
			byte[] chunkChecksum = TestUtil.createRandomArray(checksumSize);
			int chunkSize = (400 + new Random().nextInt(100)) * 1024;
			
			ChunkEntry chunkEntry = new ChunkEntry(chunkChecksum, chunkSize);
			chunks.add(chunkEntry);
		}
		
		return chunks;		
	}	
}
