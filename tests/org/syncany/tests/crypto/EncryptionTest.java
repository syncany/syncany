package org.syncany.tests.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.syncany.chunk.CipherTransformer;
import org.syncany.config.EncryptionException;
import org.syncany.config.Logging;
import org.syncany.util.StringUtil;
import org.xml.sax.helpers.DefaultHandler;

public class EncryptionTest {
	/*private static final Logger logger = Logger.getLogger(EncryptionTest.class.getSimpleName());		
	
	static {
		Logging.init();
	}
		
	@Before
	public void initAndLoadProviders() throws EncryptionException {
		Encryption.init();
		Encryption.enableUnlimitedCrypto();
	}		
	
	@Test
	public void testDefaultCryptoSuiteAvailable() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, EncryptionException {
		byte[] testKeyBytes = new byte[Encryption.DEFAULT_KEYLENGTH/8];
		byte[] testIvBytes = new byte[Encryption.DEFAULT_KEYLENGTH/8];
		
		SecretKey secretKey = new SecretKeySpec(testKeyBytes, Encryption.DEFAULT_CIPHER_ALGORITHM);
		IvParameterSpec ivSpec = new IvParameterSpec(testIvBytes);
		
		Cipher cipher = Cipher.getInstance(Encryption.DEFAULT_CIPHER_STRING, Encryption.PROVIDER);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
		
		byte[] plaintext = new byte[2048];
		byte[] ciphertext = cipher.doFinal(plaintext);
		
		cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
		byte[] decrypted = cipher.doFinal(ciphertext);
		
		assertArrayEquals(plaintext, decrypted);		
	}
	
	@Test
	public void testEncryptionWithDefaultSettings() throws Exception {
		Encryption encryptionSettings = new Encryption();
		encryptionSettings.setPassword("some password");
		
		doTestEncryption(encryptionSettings);
	}	
	
	@Test
	public void testEncryptionWithDes64CbcPkcs5() throws Exception {
		Encryption encryptionSettings = new Encryption();
		
		encryptionSettings.setCipherStr("DES/CBC/PKCS5Padding");
		encryptionSettings.setKeySize(64);
		encryptionSettings.setPassword("some password");
		
		doTestEncryption(encryptionSettings);
	}		
	
	@Test
	@Ignore
	public void testEncryptionWith3Des112CbcPkcs5() throws Exception {
		Encryption encryptionSettings = new Encryption();
		
		encryptionSettings.setCipherStr("DESede/CBC/PKCS5Padding");
		encryptionSettings.setKeySize(112);
		encryptionSettings.setPassword("some password");
		encryptionSettings.setIvNeeded(true);
		
		doTestEncryption(encryptionSettings);

		// TODO [low] Test fails: 3DES needs special handling b/c key sizes are used differently
	}		
	
	@Test
	public void testEncryptionWithAes128EcbPkcs5() throws Exception {
		Encryption encryptionSettings = new Encryption();
		
		encryptionSettings.setCipherStr("AES/ECB/PKCS5Padding");
		encryptionSettings.setKeySize(128);
		encryptionSettings.setPassword("some password");
		encryptionSettings.setIvNeeded(false);
		
		doTestEncryption(encryptionSettings);
	}		
	
	@Test
	public void testUnlimitedCrypto() throws Exception {
		try {
			Cipher cipher = Cipher.getInstance("AES", Encryption.PROVIDER);
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(new byte[256/8], "AES"));
		}
		catch (Exception e) {
			fail("Unlimited crypto not available. Enable policy files at: http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html");
		}		
		
		// NOTE: If this fails, it might be because 'unlimited crypto' not available.
		// Download policy files at: http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html				
	}	
	
	@Test
	public void testEncryptionWithAes192GcmNoPadding() throws Exception {
		Encryption encryptionSettings = new Encryption();
		
		encryptionSettings.setCipherStr("AES/GCM/NoPadding");
		encryptionSettings.setKeySize(192);
		encryptionSettings.setPassword("some password");
		
		doTestEncryption(encryptionSettings);
			
		// NOTE: If this fails, it might be because 'unlimited crypto' not available.
		// Download policy files at: http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html		
	}	
	
	@Test
	public void testEncryptionWithAes256GcmNoPadding() throws Exception {
		Encryption encryptionSettings = new Encryption();
		
		encryptionSettings.setCipherStr("AES/GCM/NoPadding");
		encryptionSettings.setKeySize(256);
		encryptionSettings.setPassword("some password");
		
		doTestEncryption(encryptionSettings);
		
		// NOTE: If this fails, it might be because 'unlimited crypto' not available.
		// Download policy files at: http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html		
	}	
	
	@Test
	public void testEncryptionWithTwofish256GcmNoPadding() throws Exception {
		Encryption encryptionSettings = new Encryption();
		
		encryptionSettings.setCipherStr("Twofish/GCM/NoPadding");
		encryptionSettings.setKeySize(256);
		encryptionSettings.setPassword("some password");
		
		doTestEncryption(encryptionSettings);
		
		// NOTE: If this fails, it might be because 'unlimited crypto' not available.
		// Download policy files at: http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html		
	}	
	
	@Test
	public void testSaxParserWithCipherTransformerWithAesGcm() throws Exception {
		testSaxParserWithCipherTransformer("AES/GCM/NoPadding", 128);
	}
	
	@Test
	public void testSaxParserWithCipherTransformerWithAesCbcPkcs5() throws Exception {
		testSaxParserWithCipherTransformer("AES/CBC/PKCS5Padding", 128);
	}
	
	public void testSaxParserWithCipherTransformer(String cipherStr, int keySize) throws Exception {
		String xmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<database version=\"1\">\n"
			+ "	<databaseVersions>\n"
			+ "		<databaseVersion>\n"
			+ "		</databaseVersion>\n"
			+ "	</databaseVersions>\n"
			+ "</database>";
		
		Encryption encryptionSettings = new Encryption();	
		encryptionSettings.setCipherStr(cipherStr);
		encryptionSettings.setKeySize(keySize);		
		encryptionSettings.setPassword("some password");
		
		CipherTransformer cipherTransformer = new CipherTransformer(encryptionSettings);
		
		// Test encrypt
		byte[] encryptedData = doEncrypt(xmlStr.getBytes(), cipherTransformer);

		// Test decrypt with SAX parser	
		InputStream is = cipherTransformer.createInputStream(new ByteArrayInputStream(encryptedData));
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		
		saxParser.parse(is, new DefaultHandler());	
		
		// Success if it does not throw an exception

		// Regular CipherInputStream does NOT work with GCM mode
		// GcmCompatibleCipherInputStream fixes this!
		
		// See http://bouncy-castle.1462172.n4.nabble.com/Using-AES-GCM-NoPadding-with-javax-crypto-CipherInputStream-td4655271.html
		// and http://bouncy-castle.1462172.n4.nabble.com/using-GCMBlockCipher-with-CipherInputStream-td4655147.html
	}	
	
	private void doTestEncryption(Encryption encryptionSettings) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException, EncryptionException, InvalidKeyException {
		CipherTransformer encryptCipherTransformer = new CipherTransformer(encryptionSettings);
		CipherTransformer decryptCipherTransformer = new CipherTransformer(encryptionSettings);
		
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
	
	private byte[] doEncrypt(byte[] srcData, CipherTransformer cipherTransformer) throws IOException, InvalidKeySpecException, InvalidKeyException, NoSuchAlgorithmException, EncryptionException {
		// Write 
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		OutputStream os = cipherTransformer.createOutputStream(bos);		
		
		os.write(srcData, 0, srcData.length);
		os.close();
		
		byte[] encryptedData = bos.toByteArray();
		
		return encryptedData;
	}	

	private byte[] doDecrypt(byte[] encryptedData, CipherTransformer cipherTransformer) throws IOException, InvalidKeySpecException, InvalidKeyException, NoSuchAlgorithmException, EncryptionException {
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
	
	@Test 
	@Ignore
	public void listCryptoSettingsAvailable() {
		logger.log(Level.INFO, "Listing security providers and properties:");
		
		for (Provider provider: Security.getProviders()) {
			logger.log(Level.INFO, "- Provider '"+provider.getName()+"' ");
			
			List<String> propertyNames = new ArrayList<String>();
			propertyNames.addAll(provider.stringPropertyNames());
			
			Collections.sort(propertyNames);
			
			for (String key : propertyNames) {
				logger.log(Level.INFO, "   + "+key+" = "+provider.getProperty(key));
			}
		}
	}		*/	
}
