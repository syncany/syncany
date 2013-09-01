package org.syncany.tests.chunk;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.chunk.Chunk;
import org.syncany.chunk.Chunker;
import org.syncany.chunk.CustomMultiChunker;
import org.syncany.chunk.FixedOffsetChunker;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.MultiChunker;
import org.syncany.chunk.TarMultiChunker;
import org.syncany.tests.util.TestFileUtil;

public class MultiChunkerTest {

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
	public void testChunkFileIntoMultiChunks() throws IOException {
		int minMultiChunkSize = 512 * 1024;
		int chunkSizeB = 16000;

		Chunker[] chunkers = new Chunker[] { 
			new FixedOffsetChunker(chunkSizeB)
		};
		
		MultiChunker[] multiChunkers = new MultiChunker[] { 
			new CustomMultiChunker(minMultiChunkSize),
			new TarMultiChunker(minMultiChunkSize) 
		};
		
		for (Chunker chunker : chunkers) {
			for (MultiChunker multiChunker : multiChunkers) {
				testChunkFileIntoMultiChunks(chunker, multiChunker, minMultiChunkSize);
			}
		}
	}
	
	public void testChunkFileIntoMultiChunks(Chunker chunker, MultiChunker multiChunker, int minMultiChunkSize) throws IOException {
		int fileSizeBig = 4560000;
		int fileSizeSmall = 1230;
		int fileAmountSizeSmall = 2;
		int fileAmountSizeBig = 3;

		List<File> files = TestFileUtil.generateRandomBinaryFilesInDirectory(tempDir, fileSizeSmall, fileAmountSizeSmall);
		files.addAll(TestFileUtil.generateRandomBinaryFilesInDirectory(tempDir, fileSizeBig, fileAmountSizeBig));

		Set<MultiChunk> resultMultiChunks = chunkFileIntoMultiChunks(files, chunker, multiChunker);

		long totalFilesSize = (fileSizeBig * fileAmountSizeBig) + (fileSizeSmall * fileAmountSizeSmall);
		assertEquals((totalFilesSize / (minMultiChunkSize)), resultMultiChunks.size());
	}

	private Set<MultiChunk> chunkFileIntoMultiChunks(List<File> files, Chunker foc, MultiChunker customMultiChunker)
			throws IOException {

		Set<MultiChunk> resultMultiChunks = new HashSet<MultiChunk>();

		MultiChunk customMultiChunk = createNewMultiChunk(customMultiChunker);

		for (File file : files) {
			Enumeration<Chunk> chunks = foc.createChunks(file);
			while (chunks.hasMoreElements()) {
				Chunk chunk = chunks.nextElement();
				customMultiChunk.write(chunk);

				if (customMultiChunk.isFull()) {
					customMultiChunk.close();
					resultMultiChunks.add(customMultiChunk);
					customMultiChunk = createNewMultiChunk(customMultiChunker);
				}
			}
		}
		customMultiChunk.close();
		resultMultiChunks.add(customMultiChunk);

		return resultMultiChunks;
	}

	private MultiChunk createNewMultiChunk(MultiChunker customMultiChunker) {
		FileOutputStream fos;
		String multiChunkName = String.valueOf(new Random().nextInt());

		MultiChunk customChunk = null;
		try {
			fos = new FileOutputStream(tempDir.getAbsolutePath() + "/MultiChunk" + multiChunkName);
			customChunk = customMultiChunker.createMultiChunk(multiChunkName.getBytes(), fos);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		assertArrayEquals(customChunk.getId(), multiChunkName.getBytes());

		return customChunk;
	}

}
