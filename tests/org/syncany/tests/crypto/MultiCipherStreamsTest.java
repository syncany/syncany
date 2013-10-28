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

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Test;
import org.syncany.chunk.CipherTransformer;
import org.syncany.chunk.Transformer;
import org.syncany.config.Logging;
import org.syncany.crypto.CipherSpec;
import org.syncany.crypto.CipherSpecs;
import org.syncany.crypto.CipherException;
import org.syncany.util.StringUtil;
import org.xml.sax.helpers.DefaultHandler;

public class MultiCipherStreamsTest {
	private static final Logger logger = Logger.getLogger(MultiCipherStreamsTest.class.getSimpleName());		
	
	static {
		Logging.init();
	}		
	
	@Test
	public void testCipherSuiteOneAndTwo() throws Exception {
		doTestEncryption(
			Arrays.asList(new CipherSpec[] {
				CipherSpecs.getCipherSpec(1),
				CipherSpecs.getCipherSpec(2)
			})
		);
	}
	
	@Test
	public void testCipherSuiteThreeAndFour() throws Exception {
		doTestEncryption(
			Arrays.asList(new CipherSpec[] {
				CipherSpecs.getCipherSpec(3),
				CipherSpecs.getCipherSpec(4)
			})
		);
	}
	

	@Test
	public void testSaxParserWithCipherTransformerWithAesGcm() throws Exception {
		doTestEncryption(
			Arrays.asList(new CipherSpec[] {
				CipherSpecs.getCipherSpec(1),
				CipherSpecs.getCipherSpec(2)
			})
		);
	}
	
	@Test
	public void testSaxParserWithCipherTransformerWithAesCbcPkcs5() throws Exception {
		doTestEncryption(
			Arrays.asList(new CipherSpec[] {
				CipherSpecs.getCipherSpec(3),
				CipherSpecs.getCipherSpec(4)
			})
		);
	}
	
	public void testSaxParserWithMultiCipherTransformer(List<CipherSpec> cipherSuites) throws Exception {
		String xmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<database version=\"1\">\n"
			+ "	<databaseVersions>\n"
			+ "		<databaseVersion>\n"
			+ "		</databaseVersion>\n"
			+ "	</databaseVersions>\n"
			+ "	<databaseVersions>\n"
			+ "		<databaseVersion>\n"
			+ "		</databaseVersion>\n"
			+ "	</databaseVersions>\n"
			+ "	<databaseVersions>\n"
			+ "		<databaseVersion>\n"
			+ "		</databaseVersion>\n"
			+ "	</databaseVersions>\n"
			+ "	<databaseVersions>\n"
			+ "		<databaseVersion>\n"
			+ "		</databaseVersion>\n"
			+ "	</databaseVersions>\n"
			+ "</database>";
		
		
		Transformer cipherTransformer = new CipherTransformer(cipherSuites, "some password");
		
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
	
	private void doTestEncryption(List<CipherSpec> cipherSuites) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException, CipherException, InvalidKeyException {
		Transformer encryptCipherTransformer = new CipherTransformer(cipherSuites, "some password");
		Transformer decryptCipherTransformer = new CipherTransformer(cipherSuites, "some password");
		
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
	
	private byte[] doEncrypt(byte[] srcData, Transformer cipherTransformer) throws IOException, InvalidKeySpecException, InvalidKeyException, NoSuchAlgorithmException, CipherException {
		// Write 
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		OutputStream os = cipherTransformer.createOutputStream(bos);		
		
		os.write(srcData, 0, srcData.length);
		os.close();
		
		byte[] encryptedData = bos.toByteArray();
		
		return encryptedData;
	}	

	private byte[] doDecrypt(byte[] encryptedData, Transformer cipherTransformer) throws IOException, InvalidKeySpecException, InvalidKeyException, NoSuchAlgorithmException, CipherException {
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
