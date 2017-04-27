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
package org.syncany.tests.unit.chunk;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.Test;
import org.syncany.chunk.Chunk;
import org.syncany.chunk.Chunker;
import org.syncany.chunk.FixedChunker;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.MultiChunker;
import org.syncany.chunk.NoTransformer;
import org.syncany.chunk.Transformer;
import org.syncany.chunk.ZipMultiChunker;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.tests.unit.util.TestFileUtil;

public class MultiChunkerTest {
	private static Logger logger = Logger.getLogger(MultiChunkerTest.class.getSimpleName());
			
	@Test
	public void testChunkFileIntoMultiChunks() throws Exception {
		int minMultiChunkSize = 512;
		int chunkSizeB = 16000;

		Chunker[] chunkers = new Chunker[] { 
			new FixedChunker(chunkSizeB)
		};
		
		MultiChunker[] multiChunkers = new MultiChunker[] { 
			//new CustomMultiChunker(minMultiChunkSize),
			new ZipMultiChunker(minMultiChunkSize) 
		};
		
		for (Chunker chunker : chunkers) {
			for (MultiChunker multiChunker : multiChunkers) {
				logger.log(Level.INFO, "Running with "+chunker.getClass()+" and "+multiChunker.getClass());				
				chunkFileIntoMultiChunks(chunker, multiChunker, minMultiChunkSize);
			}
		}
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

		logger.log(Level.INFO, "- In "+tempDir+" ...");			

		Set<MultiChunk> resultMultiChunks = chunkFileIntoMultiChunks(tempDir, files, chunker, multiChunker, new NoTransformer());

		long totalFilesSize = (fileSizeBig * fileAmountSizeBig) + (fileSizeSmall * fileAmountSizeSmall);
		assertEquals(totalFilesSize / minMultiChunkSize / 1024, resultMultiChunks.size());
		
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
		MultiChunkId multiChunkId = MultiChunkId.secureRandomMultiChunkId();

		MultiChunk customChunk = null;
		try {
			fos = new FileOutputStream(tempDir.getAbsolutePath() + "/MultiChunk" + multiChunkId);
			customChunk = customMultiChunker.createMultiChunk(multiChunkId, transformer.createOutputStream(fos));
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		assertEquals(customChunk.getId(), multiChunkId);

		return customChunk;
	}
}
