package org.syncany.tests.chunk;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

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
import org.syncany.chunk.FixedOffsetChunker;
import org.syncany.chunk.GzipCompressor;
import org.syncany.chunk.MultiChunker;
import org.syncany.chunk.NoTransformer;
import org.syncany.chunk.TTTDChunker;
import org.syncany.chunk.Transformer;
import org.syncany.tests.TestUtil;

public class FrameworkCombinationTests {
	private File tempDir;
	private List<FrameworkCombination> combinations;

	private class FrameworkCombination {
		String name;
		Chunker chunker;
		MultiChunker multiChunker;
		Transformer transformer;

		public FrameworkCombination(String name, Chunker chunker, MultiChunker multiChunker,
				Transformer transformerChain) {
			
			this.name = name;
			this.chunker = chunker;
			this.multiChunker = multiChunker;
			this.transformer = transformerChain;
		}
	}

	@Before
	public void setUp() throws Exception {
		tempDir = TestUtil.createTempDirectoryInSystemTemp();
		combinations = new ArrayList<FrameworkCombination>();

		fillCombinations();
	}

	private void fillCombinations() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		// MultiChunks
		List<MultiChunker> MultiChunkers = new LinkedList<MultiChunker>();

		int[] sleepMillis = new int[] { 0, 20 };
		int[] MultiChunkSizes = new int[] { 250, 500 };

		for (int j = 0; j < sleepMillis.length; j++) {
			for (int i = 0; i < MultiChunkSizes.length; i++) {
				MultiChunkers.add(new CustomMultiChunker(MultiChunkSizes[i], sleepMillis[j]));
			}
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
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(
				new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }, "AES"));

		List<Transformer> transformerChains = new LinkedList<Transformer>();

		transformerChains.add(new NoTransformer());
		transformerChains.add(new CipherEncrypter(cipher));
		transformerChains.add(new GzipCompressor(new CipherEncrypter(cipher)));

		for (MultiChunker MultiChunker : MultiChunkers) {
			for (Transformer transformer : transformerChains) {
				for (Chunker chunker : chunkers) {
					String configName = MultiChunker + "/" + chunker + "/" + transformer;
					combinations.add(new FrameworkCombination(configName, chunker, MultiChunker, transformer));
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
		List<File> inputFiles = TestUtil.generateRandomBinaryFilesInDirectory(tempDir, 10*1024, 1000);
		
		for(FrameworkCombination combination : combinations) {
			testCombination(inputFiles, combination);
		}
	}
	
	private void testCombination(List<File> inputFiles, FrameworkCombination combination) throws Exception {
		Chunker chunker = combination.chunker;
		MultiChunker multiChunker = combination.multiChunker;
		Transformer transformer = combination.transformer;
		
		File outputFile = TestUtil.createRandomFilenameInDirectory(tempDir);
		FileOutputStream outputFileStream = new FileOutputStream(outputFile);
		
		Deduper deduper = new Deduper(chunker, multiChunker, transformer);
		//TODO deduper.deduplicate(inputFiles,
	}

}
