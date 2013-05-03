package org.syncany.tests.chunk;

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
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.syncany.chunk.Chunk;
import org.syncany.chunk.Chunker;
import org.syncany.chunk.CipherEncrypter;
import org.syncany.chunk.CustomMultiChunker;
import org.syncany.chunk.Deduper;
import org.syncany.chunk.DeduperAdapter;
import org.syncany.chunk.FixedOffsetChunker;
import org.syncany.chunk.GzipCompressor;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.MultiChunker;
import org.syncany.chunk.NoTransformer;
import org.syncany.chunk.TTTDChunker;
import org.syncany.chunk.Transformer;
import org.syncany.tests.TestUtil;
import org.syncany.util.ByteArray;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class FrameworkCombinationTests {
	private static final Logger logger = Logger.getLogger(FrameworkCombinationTests.class.getSimpleName());
	
	private File tempDir;	
	private List<FrameworkCombination> combinations;

	private class FrameworkCombination {
		private String name;
		private Chunker chunker;
		private MultiChunker multiChunker;
		private Transformer transformer;

		public FrameworkCombination(String name, Chunker chunker, MultiChunker multiChunker, Transformer transformerChain) {		
			this.name = name;
			this.chunker = chunker;
			this.multiChunker = multiChunker;
			this.transformer = transformerChain;
		}
	}
	
	private class ChunkIndex {
		private Map<File, List<ByteArray>> inputFileToChunkIDs = new HashMap<File, List<ByteArray>>();
		private Map<ByteArray, ByteArray> chunkIDToMultiChunkID = new HashMap<ByteArray, ByteArray>();
		private List<File> outputMultiChunkFiles = new ArrayList<File>();
	}

	@Before
	public void setUp() throws Exception {
		tempDir = TestUtil.createTempDirectoryInSystemTemp();
		combinations = new ArrayList<FrameworkCombination>();

		fillCombinations();
	}

	private void fillCombinations() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		// MultiChunks
		List<MultiChunker> multiChunkers = new LinkedList<MultiChunker>();

		int[] multiChunkSizes = new int[] { 250000, 500000 };

		for (int i = 0; i < multiChunkSizes.length; i++) {
			multiChunkers.add(new CustomMultiChunker(multiChunkSizes[i]));
		}

		// Chunks
		List<Chunker> chunkers = new LinkedList<Chunker>();

		int[] chunkSizes = new int[] { 8000, 16000 };
		String[] digestAlgs = new String[] { "MD5"/* , "SHA1" */};
		String[] fingerprinters = new String[] { "Adler32" /*, "Plain", "Rabin"*/ };

		for (int i = 0; i < chunkSizes.length; i++) {
			for (int j = 0; j < digestAlgs.length; j++) {
				chunkers.add(new FixedOffsetChunker(chunkSizes[i], digestAlgs[j]));

				for (int k = 0; k < fingerprinters.length; k++) {
					chunkers.add(new TTTDChunker(chunkSizes[i], TTTDChunker.DEFAULT_WINDOW_SIZE, digestAlgs[j], fingerprinters[k]));
				}
			}
		}

		// Compression/Encryption
		Cipher encryptCipher = Cipher.getInstance("AES");
		encryptCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(
				new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }, "AES"));

		Cipher decryptCipher = Cipher.getInstance("AES");
		decryptCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(
				new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }, "AES"));

		List<Transformer> transformerChains = new LinkedList<Transformer>();

		transformerChains.add(new NoTransformer());
		transformerChains.add(new CipherEncrypter(encryptCipher, decryptCipher));
		transformerChains.add(new GzipCompressor());
		transformerChains.add(new GzipCompressor(new CipherEncrypter(encryptCipher, decryptCipher)));

		for (MultiChunker multiChunker : multiChunkers) {
			for (Transformer transformer : transformerChains) {
				for (Chunker chunker : chunkers) {
					String configName = multiChunker + "/" + chunker + "/" + transformer;
					combinations.add(new FrameworkCombination(configName, chunker, multiChunker, transformer));
				}
			}
		}
	}

	@After
	public void tearDown() throws Exception {
		TestUtil.deleteDirectory(tempDir);
	}

	@Test
	public void testBlackBoxCombinationsWith50KBInputFile() throws Exception {
		List<File> inputFiles = TestUtil.generateRandomBinaryFilesInDirectory(tempDir, 10*1024, 5);
		Map<File, ByteArray> inputFilesWithChecksums = TestUtil.createChecksums(inputFiles);
		
		for(FrameworkCombination combination : combinations) {
			logger.info("Testing "+combination.name+" ...");
			testBlackBoxCombination(inputFilesWithChecksums, combination);
		}
	}
		
	private void testBlackBoxCombination(Map<File, ByteArray> inputFilesWithChecksums, FrameworkCombination combination) throws Exception {		
		final List<File> inputFiles = new ArrayList<File>(inputFilesWithChecksums.keySet());
		
		// Deduplicate
		ChunkIndex chunkIndex = deduplicateAndCreateChunkIndex(inputFiles, combination);
		
		// Assemble
		Map<ByteArray, File> extractedChunkIDToChunkFile = extractChunksFromMultiChunks(chunkIndex.outputMultiChunkFiles, combination);
		Map<File, File> inputFilesToReassembledOutputFiles = reassembleFiles(chunkIndex.inputFileToChunkIDs, extractedChunkIDToChunkFile);		
		
		// Compare checksums of files
		for (Map.Entry<File, File> inputFilesToReassembledOutputFilesEntry : inputFilesToReassembledOutputFiles.entrySet()) {
			File inputFile = inputFilesToReassembledOutputFilesEntry.getKey();
			File outputFile = inputFilesToReassembledOutputFilesEntry.getValue();
			
			byte[] inputFileChecksum = TestUtil.createChecksum(inputFile);
			byte[] outputFileChecksum = TestUtil.createChecksum(outputFile);
			
			assertArrayEquals("Input file and output file checksums do not match" +
					"for files "+inputFile+" and "+outputFile, inputFileChecksum, outputFileChecksum);
		}
	}

	private ChunkIndex deduplicateAndCreateChunkIndex(final List<File> inputFiles, FrameworkCombination combination) throws IOException {
		final ChunkIndex chunkIndex = new ChunkIndex();
		
		Deduper deduper = new Deduper(combination.chunker, combination.multiChunker, combination.transformer);
		deduper.deduplicate(inputFiles, new DeduperAdapter() {			
			@Override
			public void onWriteMultiChunk(MultiChunk multiChunk, Chunk chunk) {
				chunkIndex.chunkIDToMultiChunkID.put(new ByteArray(chunk.getChecksum()), new ByteArray(multiChunk.getId()));				
			}								
			
			@Override
			public void onFileAddChunk(File file, Chunk chunk) {
				List<ByteArray> chunkIDsForFile = chunkIndex.inputFileToChunkIDs.get(file);
				
				if (chunkIDsForFile == null) {
					chunkIDsForFile = new ArrayList<ByteArray>();
				}
				
				chunkIDsForFile.add(new ByteArray(chunk.getChecksum()));
				chunkIndex.inputFileToChunkIDs.put(file, chunkIDsForFile);
			}
			
			@Override
			public boolean onChunk(Chunk chunk) {
				if (chunkIndex.chunkIDToMultiChunkID.containsKey(new ByteArray(chunk.getChecksum()))) {
					return false;
				}
				else {
					return true;
				}
			}
			
			@Override
			public File getMultiChunkFile(byte[] multiChunkId) {
				File outputMultiChunk = new File(tempDir+"/multichunk-"+StringUtil.toHex(multiChunkId));
				chunkIndex.outputMultiChunkFiles.add(outputMultiChunk);
				
				return outputMultiChunk;
			}
		});
		
		return chunkIndex;
	}
	

	private Map<ByteArray, File> extractChunksFromMultiChunks(List<File> outputMultiChunkFiles, FrameworkCombination combination) throws IOException {
		Map<ByteArray, File> extractedChunks = new HashMap<ByteArray, File>();
		
		for (File outputMultiChunkFile : outputMultiChunkFiles) {
			MultiChunk outputMultiChunk = combination.multiChunker.createMultiChunk(
					combination.transformer.transform(new FileInputStream(outputMultiChunkFile)));
			
			Chunk outputChunkInMultiChunk = null;
			
			while (null != (outputChunkInMultiChunk = outputMultiChunk.read())) {
				File extractedChunkFile = new File(tempDir+"/chunk-"+StringUtil.toHex((outputChunkInMultiChunk.getChecksum()))+"-from-multichunk-"+StringUtil.toHex(outputMultiChunk.getId()));
				FileUtil.writeToFile(outputChunkInMultiChunk.getContent(), extractedChunkFile);
				
				extractedChunks.put(new ByteArray(outputChunkInMultiChunk.getChecksum()), extractedChunkFile);
			}
		}		
		
		return extractedChunks;
	}
	

	private Map<File, File> reassembleFiles(Map<File, List<ByteArray>> inputFileToChunkIDs, Map<ByteArray, File> extractedChunkIDToChunkFile) throws IOException {
		Map<File, File> inputFileToOutputFile = new HashMap<File, File>();
		
		for (Map.Entry<File, List<ByteArray>> inputFileToChunkIDsEntry : inputFileToChunkIDs.entrySet()) {
			File inputFile = inputFileToChunkIDsEntry.getKey();
			List<ByteArray> chunkIDs = inputFileToChunkIDsEntry.getValue();
			
			File outputFile = new File(tempDir+"/reassembledfile-"+inputFile.getName());
			FileOutputStream outputFileOutputStream = new FileOutputStream(outputFile);
			
			for (ByteArray chunkID : chunkIDs) {
				File extractedChunkFile = extractedChunkIDToChunkFile.get(chunkID);
				FileUtil.appendToOutputStream(extractedChunkFile, outputFileOutputStream);
			}
			
			inputFileToOutputFile.put(inputFile, outputFile);
		}
		
		return inputFileToOutputFile;
	}

	@Ignore
	@Test
	public void testContentsOfOneMultiChunk() {
		// Create multichunk from one file, then assemble it again and test if it's the same
		// with input file size < multichunk size
		
//		TestUtil.generateBinaryFile()
	}
	
	@Ignore
	@Test
	public void testContentsOfTwoMultiChunks() {
		// Like above, but with input file size > multichunk size
	}	

}
