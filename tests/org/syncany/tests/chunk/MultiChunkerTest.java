package org.syncany.tests.chunk;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.Test;
import org.syncany.chunk.Chunk;
import org.syncany.chunk.Chunker;
import org.syncany.chunk.FixedOffsetChunker;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.MultiChunker;
import org.syncany.chunk.NoTransformer;
import org.syncany.chunk.Transformer;
import org.syncany.chunk.ZipMultiChunker;
import org.syncany.tests.util.TestFileUtil;

public class MultiChunkerTest {
	@Test
	public void testChunkFileIntoMultiChunks() throws Exception {
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		
		int minMultiChunkSize = 512 * 1024;
		int chunkSizeB = 16000;

		Chunker[] chunkers = new Chunker[] { 
			new FixedOffsetChunker(chunkSizeB)
		};
		
		MultiChunker[] multiChunkers = new MultiChunker[] { 
			//new CustomMultiChunker(minMultiChunkSize),
			new ZipMultiChunker(minMultiChunkSize) 
		};
		
		for (Chunker chunker : chunkers) {
			for (MultiChunker multiChunker : multiChunkers) {
				chunkFileIntoMultiChunks(chunker, multiChunker, minMultiChunkSize);
			}
		}
		
		TestFileUtil.deleteDirectory(tempDir);
	}
	
	@Test
	public void testZipRandomAccess() throws Exception {
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		File testArchive = new File(tempDir+"/testfile.zip");
		File testOutputfile = new File(tempDir+"/testoutfile.jpg");
		
		// Write test file
		ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(testArchive));
		zipOutputStream.setLevel(ZipOutputStream.STORED);
		
		for (int i=0; i<100; i++) {
			byte[] randomArray = TestFileUtil.createRandomArray(32*1024);
			
			ZipEntry zipEntry = new ZipEntry(""+i);
			zipEntry.setSize(randomArray.length);
						
			zipOutputStream.putNextEntry(zipEntry);
			zipOutputStream.write(randomArray);
			zipOutputStream.closeEntry();
		}
		
		zipOutputStream.close();
		
		// Read it randomly
		FileOutputStream testOutFileStream = new FileOutputStream(testOutputfile);
		ZipFile zipFile = new ZipFile(testArchive);
		Random random = new Random();
		
		for (int i=0; i<100; i++) {
			int randomEntryName = random.nextInt(100);
			ZipEntry zipEntry = zipFile.getEntry(""+randomEntryName);
			
			InputStream inputStream = zipFile.getInputStream(zipEntry);
			
			byte[] buffer = new byte[4096];
			int read = -1;
			
			while (-1 != (read = inputStream.read(buffer))) {
				testOutFileStream.write(buffer, 0, read);
			}
			
			inputStream.close();			
		}
		
		zipFile.close();
		testOutFileStream.close();
		
		TestFileUtil.deleteDirectory(tempDir);
	}	
	
	public void chunkFileIntoMultiChunks(Chunker chunker, MultiChunker multiChunker, int minMultiChunkSize) throws Exception {
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		
		int fileSizeBig = 4560000;
		int fileSizeSmall = 1230;
		int fileAmountSizeSmall = 2;
		int fileAmountSizeBig = 3;

		List<File> files = TestFileUtil.createRandomFilesInDirectory(tempDir, fileSizeSmall, fileAmountSizeSmall);
		files.addAll(TestFileUtil.createRandomFilesInDirectory(tempDir, fileSizeBig, fileAmountSizeBig));

		Set<MultiChunk> resultMultiChunks = chunkFileIntoMultiChunks(tempDir, files, chunker, multiChunker, new NoTransformer());

		long totalFilesSize = (fileSizeBig * fileAmountSizeBig) + (fileSizeSmall * fileAmountSizeSmall);
		assertEquals((totalFilesSize / (minMultiChunkSize)), resultMultiChunks.size());
		
		TestFileUtil.deleteDirectory(tempDir);
	}

	private Set<MultiChunk> chunkFileIntoMultiChunks(File tempDir, List<File> files, Chunker foc, MultiChunker customMultiChunker, Transformer transformer)
			throws IOException {

		Set<MultiChunk> resultMultiChunks = new HashSet<MultiChunk>();

		MultiChunk customMultiChunk = createNewMultiChunk(tempDir, customMultiChunker, transformer);

		for (File file : files) {
			Enumeration<Chunk> chunks = foc.createChunks(file);
			while (chunks.hasMoreElements()) {
				Chunk chunk = chunks.nextElement();
				customMultiChunk.write(chunk);

				if (customMultiChunk.isFull()) {
					customMultiChunk.close();
					resultMultiChunks.add(customMultiChunk);
					customMultiChunk = createNewMultiChunk(tempDir, customMultiChunker, transformer);
				}
			}
		}
		customMultiChunk.close();
		resultMultiChunks.add(customMultiChunk);

		return resultMultiChunks;
	}

	private MultiChunk createNewMultiChunk(File tempDir, MultiChunker customMultiChunker, Transformer transformer) {
		FileOutputStream fos;
		String multiChunkName = String.valueOf(new Random().nextInt());

		MultiChunk customChunk = null;
		try {
			fos = new FileOutputStream(tempDir.getAbsolutePath() + "/MultiChunk" + multiChunkName);
			customChunk = customMultiChunker.createMultiChunk(multiChunkName.getBytes(), 
					transformer.createOutputStream(fos));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		assertArrayEquals(customChunk.getId(), multiChunkName.getBytes());

		return customChunk;
	}

}
