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
import static org.junit.Assert.assertTrue;
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

public class FixedOffsetChunkerTest {
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
		final int CHUNK_SIZE = 512 * 1024;

		Chunker chunker = new FixedChunker(CHUNK_SIZE);
		assertEquals("Other toString() result expected.", "Fixed-" + CHUNK_SIZE + "-" + FixedChunker.DEFAULT_DIGEST_ALG, chunker.toString());
	}

	@Test
	public void testCreateChunksFrom5MBFileAndTestChunkSize() throws Exception {
		// Test Constants
		final int TOTAL_FILE_SIZE = 5 * 1024 * 1024;
		final int EXACT_CHUNK_SIZE = 512 * 1024;
		final int EXPECTED_NUMBER_OF_CHUNKS = TOTAL_FILE_SIZE / EXACT_CHUNK_SIZE;
		final int EXPECTED_CHUNK_SIZE = EXACT_CHUNK_SIZE;

		// Setup
		File inputRandom5MBFile = TestFileUtil.createRandomFileInDirectory(tempDir, TOTAL_FILE_SIZE);

		File outputCopyOfRandom5MBFile = TestFileUtil.getRandomFilenameInDirectory(tempDir);
		FileOutputStream outputCopyOfRandom5MBFileOutputStream = new FileOutputStream(outputCopyOfRandom5MBFile);

		Chunker chunker = new FixedChunker(EXACT_CHUNK_SIZE, FixedChunker.DEFAULT_DIGEST_ALG);

		// Create chunks
		int actualChunkCount = 0;
		Enumeration<Chunk> chunkEnumeration = chunker.createChunks(inputRandom5MBFile);
		Chunk lastChunk = null;

		while (chunkEnumeration.hasMoreElements()) {
			actualChunkCount++;
			lastChunk = chunkEnumeration.nextElement();

			// Chunk size & checksum
			assertEquals("Chunk does not have the expected size.", EXPECTED_CHUNK_SIZE, lastChunk.getSize());
			assertNotNull("Chunk checksum should not be null.", lastChunk.getChecksum());

			outputCopyOfRandom5MBFileOutputStream.write(lastChunk.getContent());
		}

		outputCopyOfRandom5MBFileOutputStream.close();

		// Number of chunks
		assertEquals("Unexpected number of chunks when chunking", EXPECTED_NUMBER_OF_CHUNKS, actualChunkCount);

		// Checksums
		byte[] inputFileChecksum = FileUtil.createChecksum(inputRandom5MBFile, FixedChunker.DEFAULT_DIGEST_ALG);
		byte[] outputFileChecksum = FileUtil.createChecksum(outputCopyOfRandom5MBFile, FixedChunker.DEFAULT_DIGEST_ALG);

		assertArrayEquals("Checksums of input and output file do not match.", inputFileChecksum, outputFileChecksum);
		assertArrayEquals("Last chunk's getFileChecksum() should be the file checksum.", inputFileChecksum, lastChunk.getFileChecksum());
	}

	@Test
	@Ignore
	public void testChunkChecksum() {
		fail("Not yet implemented");
	}

	@Test
	public void testNextChunkEvenIfThereAreNone() throws IOException {
		// Test Constants
		final int TOTAL_FILE_SIZE = 5 * 1024;
		final int EXACT_CHUNK_SIZE = 512 * 1024;

		// Setup
		File inputFile = TestFileUtil.createRandomFileInDirectory(tempDir, TOTAL_FILE_SIZE);
		Chunker chunker = new FixedChunker(EXACT_CHUNK_SIZE);

		// Create chunks
		Enumeration<Chunk> chunkEnumeration = chunker.createChunks(inputFile);

		while (chunkEnumeration.hasMoreElements()) {
			chunkEnumeration.nextElement();
		}

		// This should lead to an IOException
		assertNull("No chunk expected, but data received.", chunkEnumeration.nextElement());
		assertFalse("hasElements() should return 'false' if no chunk available.", chunkEnumeration.hasMoreElements());
	}

	@Test
	public void testExceptionInvalidDigestAlgorithm() {
		boolean exceptionThrown = false;

		try {
			new FixedChunker(1337, "does-not-exist").createChunks(new File("/some/file"));
		}
		catch (Exception e) {
			exceptionThrown = true;
		}

		assertTrue("Exception expected.", exceptionThrown);
	}

}
