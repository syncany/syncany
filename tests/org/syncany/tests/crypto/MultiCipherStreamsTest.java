package org.syncany.tests.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;
import org.syncany.chunk.MultiCipherTransformer;
import org.syncany.chunk.Transformer;
import org.syncany.config.Encryption;
import org.syncany.config.EncryptionException;
import org.syncany.config.Logging;
import org.syncany.crypto.CipherSuite;
import org.syncany.crypto.CipherSuites;
import org.syncany.util.StringUtil;

public class MultiCipherStreamsTest {
	private static final Logger logger = Logger.getLogger(MultiCipherStreamsTest.class.getSimpleName());		
	
	static {
		Logging.init();
	}
		
	@Before
	public void initAndLoadProviders() throws EncryptionException {
		Encryption.init();
		Encryption.enableUnlimitedCrypto();
	}			
	
	@Test
	public void testCipherSuiteOneAndTwo() throws Exception {
		doTestEncryption(
			Arrays.asList(new CipherSuite[] {
				CipherSuites.getCipherSuite(1),
				CipherSuites.getCipherSuite(2)
			})
		);
	}
	
	@Test
	public void testCipherSuiteThreeAndFour() throws Exception {
		doTestEncryption(
			Arrays.asList(new CipherSuite[] {
				CipherSuites.getCipherSuite(3),
				CipherSuites.getCipherSuite(4)
			})
		);
	}
	private void doTestEncryption(List<CipherSuite> cipherSuites) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException, EncryptionException, InvalidKeyException {
		Transformer encryptCipherTransformer = new MultiCipherTransformer(cipherSuites, "some password");
		Transformer decryptCipherTransformer = new MultiCipherTransformer(cipherSuites, "some password");
		
		// Prepare data
		byte[] srcData = new byte[1024];
		
		for (int i=0;i<srcData.length; i++) {
			srcData[i] = (byte)(i & 0xff);
		}				
		
		byte[] encryptedData1 = doEncrypt(srcData, encryptCipherTransformer);
		byte[] decryptedData1 = doDecrypt(encryptedData1, decryptCipherTransformer);
		
		byte[] encryptedData2 = doEncrypt(srcData, encryptCipherTransformer);
		byte[] decryptedData2 = doDecrypt(encryptedData2, decryptCipherTransformer);
		
		logger.log(Level.INFO, "Source Data:              "+StringUtil.toHex(srcData));
		logger.log(Level.INFO, "Decrypted Data (Round 1): "+StringUtil.toHex(decryptedData1));		
		logger.log(Level.INFO, "Decrypted Data (Round 2): "+StringUtil.toHex(decryptedData2));
		logger.log(Level.INFO, "Encrypted Data (Round 1): "+StringUtil.toHex(encryptedData1));
		logger.log(Level.INFO, "Encrypted Data (Round 2): "+StringUtil.toHex(encryptedData2));
		
		assertEquals("Encrypted and decrypted Data is different (round 1)", StringUtil.toHex(srcData), StringUtil.toHex(decryptedData1));
		assertEquals("Encrypted and decrypted Data is different (round 2)", StringUtil.toHex(srcData), StringUtil.toHex(decryptedData2));
		
		assertNotSame("Encrypted data for round 1 and 2 are identical", StringUtil.toHex(encryptedData1), StringUtil.toHex(encryptedData2));

		logger.log(Level.INFO, "Passed.");
	}
	
	private byte[] doEncrypt(byte[] srcData, Transformer cipherTransformer) throws IOException, InvalidKeySpecException, InvalidKeyException, NoSuchAlgorithmException, EncryptionException {
		// Write 
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		OutputStream os = cipherTransformer.createOutputStream(bos);		
		
		os.write(srcData, 0, srcData.length);
		os.close();
		
		byte[] encryptedData = bos.toByteArray();
		
		return encryptedData;
	}	

	private byte[] doDecrypt(byte[] encryptedData, Transformer cipherTransformer) throws IOException, InvalidKeySpecException, InvalidKeyException, NoSuchAlgorithmException, EncryptionException {
		ByteArrayOutputStream bosDecryptedData = new ByteArrayOutputStream();

		// Read		
		ByteArrayInputStream bis = new ByteArrayInputStream(encryptedData);
		InputStream is = cipherTransformer.createInputStream(bis);
		
		byte[] buffer = new byte[20];
		int read = -1;
				
		while (-1 != (read = is.read(buffer))) {
			bosDecryptedData.write(buffer, 0, read);
		}
				
		byte[] decryptedData = bosDecryptedData.toByteArray();
		
		return decryptedData;
	}	
}
