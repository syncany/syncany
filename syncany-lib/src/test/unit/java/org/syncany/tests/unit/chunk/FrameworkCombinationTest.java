/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com>
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

import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.NoSuchPaddingException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.syncany.chunk.Chunk;
import org.syncany.chunk.Chunker;
import org.syncany.chunk.CipherTransformer;
import org.syncany.chunk.Deduper;
import org.syncany.chunk.DeduperListener;
import org.syncany.chunk.GzipTransformer;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.MultiChunker;
import org.syncany.chunk.NoTransformer;
import org.syncany.chunk.Transformer;
import org.syncany.chunk.TttdChunker;
import org.syncany.chunk.ZipMultiChunker;
import org.syncany.config.Logging;
import org.syncany.crypto.CipherException;
import org.syncany.crypto.CipherSpec;
import org.syncany.crypto.CipherSpecs;
import org.syncany.crypto.CipherUtil;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.tests.unit.util.TestFileUtil;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class FrameworkCombinationTest {
	private static final Logger logger = Logger.getLogger(FrameworkCombinationTest.class.getSimpleName());

	private File tempDir;
	private List<FrameworkCombination> combinations;

	private SaltedSecretKey masterKey;

	static {
		Logging.init();
	}

	@Before
	public void initMasterKey() throws CipherException {
		masterKey = CipherUtil.createMasterKey("some password");
	}

	@Test
	public void testBlackBoxCombinationsWith50KBInputFile() throws Exception {
		// Setup
		setup();

		// Test
		List<File> inputFiles = TestFileUtil.createRandomFilesInDirectory(tempDir, 10 * 1024, 5);

		for (FrameworkCombination combination : combinations) {
			logger.info("");
			logger.info("Testing framework combination " + combination.name + " ...");
			logger.info("---------------------------------------------------------------");

			testBlackBoxCombination(inputFiles, combination);
		}

		// Tear down (if success)
		teardown();
	}

	public void setup() throws Exception {
		tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		combinations = new ArrayList<FrameworkCombination>();

		fillCombinations();
	}

	private void fillCombinations() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		// MultiChunks
		List<MultiChunker> multiChunkers = new LinkedList<MultiChunker>();

		int[] multiChunkSizes = new int[] { 250000, 500000 };

		for (int i = 0; i < multiChunkSizes.length; i++) {
			//multiChunkers.add(new CustomMultiChunker(multiChunkSizes[i]));
			multiChunkers.add(new ZipMultiChunker(multiChunkSizes[i]));
		}

		// Chunks
		List<Chunker> chunkers = new LinkedList<Chunker>();

		int[] chunkSizes = new int[] { 8000, 16000 };
		String[] digestAlgs = new String[] { /*"MD5" ,*/"SHA1" };
		String[] fingerprinters = new String[] { "Adler32" /*, "Plain", "Rabin"*/};

		for (int i = 0; i < chunkSizes.length; i++) {
			for (int j = 0; j < digestAlgs.length; j++) {
				//chunkers.add(new FixedOffsetChunker(chunkSizes[i], digestAlgs[j]));

				for (int k = 0; k < fingerprinters.length; k++) {
					chunkers.add(new TttdChunker(chunkSizes[i], TttdChunker.DEFAULT_WINDOW_SIZE, digestAlgs[j], fingerprinters[k]));
				}
			}
		}

		// Compression/Encryption
		List<CipherSpec> cipherSpecs = new ArrayList<CipherSpec>();
		cipherSpecs.add(CipherSpecs.getCipherSpec(1));
		cipherSpecs.add(CipherSpecs.getCipherSpec(2));

		List<Transformer> transformerChains = new LinkedList<Transformer>();

		transformerChains.add(new NoTransformer());
		transformerChains.add(new GzipTransformer());
		transformerChains.add(new CipherTransformer(cipherSpecs, masterKey));
		transformerChains.add(new GzipTransformer(new CipherTransformer(cipherSpecs, masterKey)));

		for (MultiChunker multiChunker : multiChunkers) {
			for (Transformer transformer : transformerChains) {
				for (Chunker chunker : chunkers) {
					String configName = multiChunker + "/" + chunker + "/" + transformer;
					combinations.add(new FrameworkCombination(configName, chunker, multiChunker, transformer));
				}
			}
		}
	}

	public void teardown() throws Exception {
		TestFileUtil.deleteDirectory(tempDir);
	}

	private void testBlackBoxCombination(List<File> inputFiles, FrameworkCombination combination) throws Exception {
		// Deduplicate
		ChunkIndex chunkIndex = deduplicateAndCreateChunkIndex(inputFiles, combination);

		// Assemble
		Map<ChunkChecksum, File> extractedChunkIDToChunkFile = extractChunksFromMultiChunks(chunkIndex.outputMultiChunkFiles, combination);
		Map<File, File> inputFilesToReassembledOutputFiles = reassembleFiles(chunkIndex.inputFileToChunkIDs, extractedChunkIDToChunkFile);

		// Compare checksums of files
		for (Map.Entry<File, File> inputFilesToReassembledOutputFilesEntry : inputFilesToReassembledOutputFiles.entrySet()) {
			File inputFile = inputFilesToReassembledOutputFilesEntry.getKey();
			File outputFile = inputFilesToReassembledOutputFilesEntry.getValue();

			byte[] inputFileChecksum = TestFileUtil.createChecksum(inputFile);
			byte[] outputFileChecksum = TestFileUtil.createChecksum(outputFile);

			assertArrayEquals("Input file and output file checksums do not match" +
					"for files " + inputFile + " and " + outputFile, inputFileChecksum, outputFileChecksum);
		}
	}

	private ChunkIndex deduplicateAndCreateChunkIndex(final List<File> inputFiles, FrameworkCombination combination) throws IOException {
		logger.log(Level.INFO, "- Deduplicate and create chunk index ...");

		final ChunkIndex chunkIndex = new ChunkIndex();

		Deduper deduper = new Deduper(combination.chunker, combination.multiChunker, combination.transformer);
		deduper.deduplicate(inputFiles, new DeduperListener() {
			@Override
			public void onMultiChunkWrite(MultiChunk multiChunk, Chunk chunk) {
				logger.log(Level.INFO, "    - Adding chunk " + StringUtil.toHex(chunk.getChecksum()) + " to multichunk " + multiChunk.getId()
						+ " ...");
				chunkIndex.chunkIDToMultiChunkID.put(new ChunkChecksum(chunk.getChecksum()), multiChunk.getId());
			}

			@Override
			public void onFileAddChunk(File file, Chunk chunk) {
				logger.log(Level.INFO, "    - Adding chunk " + StringUtil.toHex(chunk.getChecksum()) + " to inputFileToChunkIDs-map for file " + file
						+ " ...");
				List<ChunkChecksum> chunkIDsForFile = chunkIndex.inputFileToChunkIDs.get(file);

				if (chunkIDsForFile == null) {
					chunkIDsForFile = new ArrayList<ChunkChecksum>();
				}

				chunkIDsForFile.add(new ChunkChecksum(chunk.getChecksum()));
				chunkIndex.inputFileToChunkIDs.put(file, chunkIDsForFile);
			}

			@Override
			public boolean onChunk(Chunk chunk) {
				if (chunkIndex.chunkIDToMultiChunkID.containsKey(new ChunkChecksum(chunk.getChecksum()))) {
					logger.log(Level.INFO, "  + Known chunk " + StringUtil.toHex(chunk.getChecksum()));
					return false;
				}
				else {
					logger.log(Level.INFO, "  + New chunk " + StringUtil.toHex(chunk.getChecksum()));
					return true;
				}
			}

			@Override
			public File getMultiChunkFile(MultiChunkId multiChunkId) {
				File outputMultiChunk = new File(tempDir + "/multichunk-" + multiChunkId);
				chunkIndex.outputMultiChunkFiles.add(outputMultiChunk);

				return outputMultiChunk;
			}

			@Override
			public MultiChunkId createNewMultiChunkId(Chunk firstChunk) {
				// Note: In the real implementation, this should be random
				return new MultiChunkId(firstChunk.getChecksum());
			}

			@Override
			public boolean onFileFilter(File file) {
				return true;
			}

			@Override
			public boolean onFileStart(File file, int index) {
				return file.isFile() && !FileUtil.isSymlink(file);
			}

			@Override
			public void onFileEnd(File file, byte[] checksum) {
				// Empty
			}

			@Override
			public void onMultiChunkOpen(MultiChunk multiChunk) {
				// Empty
			}

			@Override
			public void onMultiChunkClose(MultiChunk multiChunk) {
				// Empty
			}

			@Override
			public void onStart(int fileCount) {
				// Empty
			}

			@Override
			public void onFinish() {
				// Empty
			}
		});

		return chunkIndex;
	}

	private Map<ChunkChecksum, File> extractChunksFromMultiChunks(List<File> outputMultiChunkFiles, FrameworkCombination combination)
			throws IOException {
		Map<ChunkChecksum, File> extractedChunks = new HashMap<ChunkChecksum, File>();

		for (File outputMultiChunkFile : outputMultiChunkFiles) {
			logger.log(Level.INFO, "- Extracting multichunk " + outputMultiChunkFile + " ...");

			MultiChunk outputMultiChunk = combination.multiChunker.createMultiChunk(
					combination.transformer.createInputStream(new FileInputStream(outputMultiChunkFile)));

			Chunk outputChunkInMultiChunk = null;

			while (null != (outputChunkInMultiChunk = outputMultiChunk.read())) {
				File extractedChunkFile = new File(tempDir + "/chunk-" + StringUtil.toHex((outputChunkInMultiChunk.getChecksum()))
						+ "-from-multichunk-" + outputMultiChunk.getId());

				logger.log(Level.INFO, "  + Writing chunk " + StringUtil.toHex((outputChunkInMultiChunk.getChecksum())) + " to " + extractedChunkFile
						+ " ...");
				TestFileUtil.writeToFile(outputChunkInMultiChunk.getContent(), extractedChunkFile);

				extractedChunks.put(new ChunkChecksum(outputChunkInMultiChunk.getChecksum()), extractedChunkFile);
			}
		}

		return extractedChunks;
	}

	private Map<File, File> reassembleFiles(Map<File, List<ChunkChecksum>> inputFileToChunkIDs, Map<ChunkChecksum, File> extractedChunkIDToChunkFile)
			throws IOException {
		Map<File, File> inputFileToOutputFile = new HashMap<File, File>();

		for (Map.Entry<File, List<ChunkChecksum>> inputFileToChunkIDsEntry : inputFileToChunkIDs.entrySet()) {
			File inputFile = inputFileToChunkIDsEntry.getKey();
			List<ChunkChecksum> chunkIDs = inputFileToChunkIDsEntry.getValue();

			File outputFile = new File(tempDir + "/reassembledfile-" + inputFile.getName());
			FileOutputStream outputFileOutputStream = new FileOutputStream(outputFile);

			logger.log(Level.INFO, "- Reassemble file " + inputFile + " to " + outputFile + " ...");

			for (ChunkChecksum chunkID : chunkIDs) {
				File extractedChunkFile = extractedChunkIDToChunkFile.get(chunkID);

				logger.log(Level.INFO, "  + Appending " + chunkID + " (file: " + extractedChunkFile + ") to " + outputFile + " ...");
				IOUtils.copy(new FileInputStream(extractedChunkFile), outputFileOutputStream);
			}

			inputFileToOutputFile.put(inputFile, outputFile);
		}

		return inputFileToOutputFile;
	}

	private static class FrameworkCombination {
		private String name;
		private Chunker chunker;
		private MultiChunker multiChunker;
		private Transformer transformer;

		public FrameworkCombination(String name, Chunker chunker, MultiChunker multiChunker, Transformer transformerChain) {
			this.name = name;
			this.chunker = chunker;
			this.multiChunker = multiChunker;
			transformer = transformerChain;
		}
	}

	private static class ChunkIndex {
		private Map<File, List<ChunkChecksum>> inputFileToChunkIDs = new HashMap<File, List<ChunkChecksum>>();
		private Map<ChunkChecksum, MultiChunkId> chunkIDToMultiChunkID = new HashMap<ChunkChecksum, MultiChunkId>();
		private List<File> outputMultiChunkFiles = new ArrayList<File>();
	}

}
