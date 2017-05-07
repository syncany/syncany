/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.chunk;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.util.FileUtil;

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
		
		Chunker chunker = new TttdChunker(CHUNK_SIZE);
		assertEquals("Other toString() result expected.", "TTTD-"+CHUNK_SIZE+"-"+TttdChunker.DEFAULT_DIGEST_ALG+"-"+TttdChunker.DEFAULT_FINGERPRINT_ALG, chunker.toString());
	}
	
	@Test
	public void testCreateChunksFrom5MBFileAndTestChunkSize() throws Exception {
		// Test Constants
		final int TOTAL_FILE_SIZE = 5*1024*1024;
		final int AVG_CHUNK_SIZE = 512*1024;
		
		// Setup				
		File inputRandom5MBFile = TestFileUtil.createRandomFileInDirectory(tempDir, TOTAL_FILE_SIZE);
		
		File outputCopyOfRandom5MBFile = TestFileUtil.getRandomFilenameInDirectory(tempDir);
		FileOutputStream outputCopyOfRandom5MBFileOutputStream = new FileOutputStream(outputCopyOfRandom5MBFile);
		
		Chunker chunker = new TttdChunker(AVG_CHUNK_SIZE);
		
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
		// TODO [low] How to test the number of chunks?

		// Checksums
		byte[] inputFileChecksum = FileUtil.createChecksum(inputRandom5MBFile, TttdChunker.DEFAULT_DIGEST_ALG);
		byte[] outputFileChecksum = FileUtil.createChecksum(outputCopyOfRandom5MBFile, TttdChunker.DEFAULT_DIGEST_ALG);
		
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
		Chunker chunker = new TttdChunker(CHUNK_SIZE);
		
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
