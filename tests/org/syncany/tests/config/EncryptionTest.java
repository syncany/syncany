package org.syncany.tests.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.syncany.chunk.CipherTransformer;
import org.syncany.config.Config;
import org.syncany.config.Encryption;
import org.syncany.config.EncryptionException;
import org.syncany.util.StringUtil;

public class EncryptionTest {
	private static final Logger logger = Logger.getLogger(EncryptionTest.class.getSimpleName());	

	static {
		Config.initLogging();
	}
	
	@Test
	public void testEncryptionWithDifferentCipherTransformers() {
		fail("Implement this");
	}
	
	@Test
	public void testEncryptionWithNonDefaultCipher() {
		fail("Implement this");
	}	
	
	@Test
	public void testEncryptionSameCipherTransformer() throws Exception {
		// Setup encryption
		Encryption e = new Encryption();
		e.setPassword("some password");
		
		CipherTransformer cipherTransformer = new CipherTransformer(e);
		
		// Prepare data
		byte[] srcData = new byte[1024];
		
		for (int i=0;i<srcData.length; i++) {
			srcData[i] = (byte)(i & 0xff);
		}				
		
		byte[] encryptedData1 = doEncrypt(srcData, cipherTransformer);
		byte[] decryptedData1 = doDecrypt(encryptedData1, cipherTransformer);
		
		byte[] encryptedData2 = doEncrypt(srcData, cipherTransformer);
		byte[] decryptedData2 = doDecrypt(encryptedData2, cipherTransformer);
		
		logger.log(Level.INFO, "Source Data:              "+StringUtil.toHex(srcData));
		logger.log(Level.INFO, "Encrypted Data (Round 1): "+StringUtil.toHex(encryptedData1));
		logger.log(Level.INFO, "Decrypted Data (Round 1): "+StringUtil.toHex(decryptedData1));		
		logger.log(Level.INFO, "Encrypted Data (Round 2): "+StringUtil.toHex(encryptedData2));
		logger.log(Level.INFO, "Decrypted Data (Round 2): "+StringUtil.toHex(decryptedData2));
		
		assertEquals("Encrypted and decrypted Data is different (round 1)", StringUtil.toHex(srcData), StringUtil.toHex(decryptedData1));
		assertEquals("Encrypted and decrypted Data is different (round 2)", StringUtil.toHex(srcData), StringUtil.toHex(decryptedData2));
		
		assertNotSame("Encrypted data for round 1 and 2 are identical", StringUtil.toHex(encryptedData1), StringUtil.toHex(encryptedData2));

		logger.log(Level.INFO, "Passed.");
	}	
	
	private byte[] doEncrypt(byte[] srcData, CipherTransformer cipherTransformer) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, EncryptionException {
		// Write 
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		OutputStream os = cipherTransformer.createOutputStream(bos);		
		
		os.write(srcData, 0, srcData.length);
		os.close();
		
		byte[] encryptedData = bos.toByteArray();
		
		return encryptedData;
	}	

	private byte[] doDecrypt(byte[] encryptedData, CipherTransformer cipherTransformer) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, EncryptionException {
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
