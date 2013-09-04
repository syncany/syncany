package org.syncany.tests.chunk;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.syncany.chunk.Chunk;
import org.syncany.chunk.Chunker;
import org.syncany.chunk.TTTDChunker;
import org.syncany.tests.util.TestFileUtil;

public class TTTDChunkerTest {
	private File tempDir;

	@Before
	public void setUp() throws Exception {
		tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
	}

	@After
	public void tearDown() throws Exception {
		TestFileUtil.deleteDirectory(tempDir);
	}

	@Test
	public void testStringSerialization() {
		final int CHUNK_SIZE = 512*1024;
		
		Chunker chunker = new TTTDChunker(CHUNK_SIZE);
		assertEquals("Other toString() result expected.", "TTTD-"+CHUNK_SIZE+"-"+TTTDChunker.DEFAULT_DIGEST_ALG+"-"+TTTDChunker.DEFAULT_FINGERPRINT_ALG, chunker.toString());
	}
	
	@Test
	public void testCreateChunksFrom5MBFileAndTestChunkSize() throws Exception {
		// Test Constants
		final int TOTAL_FILE_SIZE = 5*1024*1024;
		final int AVG_CHUNK_SIZE = 512*1024;
		
		// Setup				
		File inputRandom5MBFile = TestFileUtil.createRandomFileInDirectory(tempDir, TOTAL_FILE_SIZE);
		
		File outputCopyOfRandom5MBFile = TestFileUtil.createRandomFileInDirectory(tempDir);
		FileOutputStream outputCopyOfRandom5MBFileOutputStream = new FileOutputStream(outputCopyOfRandom5MBFile);
		
		Chunker chunker = new TTTDChunker(AVG_CHUNK_SIZE);
		
		// Create chunks
		Enumeration<Chunk> chunkEnumeration = chunker.createChunks(inputRandom5MBFile);
		Chunk lastChunk = null;
		
		while (chunkEnumeration.hasMoreElements()) {
			lastChunk = chunkEnumeration.nextElement();
			
			// Checksum
			assertNotNull("Chunk checksum should not be null.", lastChunk.getChecksum());
			
			outputCopyOfRandom5MBFileOutputStream.write(lastChunk.getContent(), 0, lastChunk.getSize());
		}
		
		outputCopyOfRandom5MBFileOutputStream.close();
		
		// Number of chunks
		// TODO How to test the number of chunks?

		// Checksums
		byte[] inputFileChecksum = TestFileUtil.createChecksum(inputRandom5MBFile, TTTDChunker.DEFAULT_DIGEST_ALG);
		byte[] outputFileChecksum = TestFileUtil.createChecksum(outputCopyOfRandom5MBFile, TTTDChunker.DEFAULT_DIGEST_ALG);
		
		assertArrayEquals("Checksums of input and output file do not match.", inputFileChecksum, outputFileChecksum);
		assertArrayEquals("Last chunk's getFileChecksum() should be the file checksum.", inputFileChecksum, lastChunk.getFileChecksum());
	}
	
	@Test
	@Ignore
	public void testNumberAndSizeOfCreatedChunks() {
		fail("Not yet implemented");
	}
	
	@Test
	public void testNextChunkEvenIfThereAreNone() throws IOException {
		// Test Constants
		final int TOTAL_FILE_SIZE = 5*1024;
		final int CHUNK_SIZE = 512*1024;
		
		// Setup				
		File inputFile = TestFileUtil.createRandomFileInDirectory(tempDir, TOTAL_FILE_SIZE);		
		Chunker chunker = new TTTDChunker(CHUNK_SIZE);
		
		// Create chunks
		Enumeration<Chunk> chunkEnumeration = chunker.createChunks(inputFile);
		
		while (chunkEnumeration.hasMoreElements()) {
			chunkEnumeration.nextElement();
		}
		
		// This should lead to an IOException
		assertNull("No chunk expected, but data received.", chunkEnumeration.nextElement());
		assertFalse("hasElements() should return 'false' if no chunk available.", chunkEnumeration.hasMoreElements());
	}
}
