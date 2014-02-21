/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Before;
import org.junit.Test;
import org.syncany.chunk.CipherTransformer;
import org.syncany.chunk.Transformer;
import org.syncany.config.Logging;
import org.syncany.crypto.CipherException;
import org.syncany.crypto.CipherSpec;
import org.syncany.crypto.CipherSpecs;
import org.syncany.crypto.MultiCipherOutputStream;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.util.StringUtil;
import org.xml.sax.helpers.DefaultHandler;

public class MultiCipherStreamsTest {
	private static final Logger logger = Logger.getLogger(MultiCipherStreamsTest.class.getSimpleName());			
	private static SaltedSecretKey masterKey;
	
	static {
		Logging.init();
	}		
	
	@Before
	public void setup() throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		if (masterKey == null) {
			masterKey = createDummyMasterKey();
		}
	}
	
	@Test
	public void testCipherAes128AndTwofish128() throws Exception {
		doTestEncryption(
			Arrays.asList(new CipherSpec[] {
				CipherSpecs.getCipherSpec(1),
				CipherSpecs.getCipherSpec(2)
			})
		);
	}
	
	@Test
	public void testCipherAes256AndTwofish256() throws Exception {
		doTestEncryption(
			Arrays.asList(new CipherSpec[] {
				CipherSpecs.getCipherSpec(3),
				CipherSpecs.getCipherSpec(4)
			})
		);
	}	
	
	@Test
	public void testHmacAvailability() throws Exception {
		Mac.getInstance(MultiCipherOutputStream.HMAC_SPEC.getAlgorithm());
		// Should not throw an exception
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
		
		
		Transformer cipherTransformer = new CipherTransformer(cipherSuites, masterKey);
		
		// Test encrypt
		byte[] encryptedData = doEncrypt(StringUtil.toBytesUTF8(xmlStr), cipherTransformer);

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
	
	private void doTestEncryption(List<CipherSpec> cipherSpecs) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException, CipherException, InvalidKeyException {
		Transformer encryptCipherTransformer = new CipherTransformer(cipherSpecs, masterKey);
		Transformer decryptCipherTransformer = new CipherTransformer(cipherSpecs, masterKey);
		
		// Prepare data
		byte[] srcData = new byte[10*1024];
		
		for (int i=0;i<srcData.length; i++) {
			srcData[i] = (byte)(i & 0xff);
		}				
		
		byte[] encryptedData1 = doEncrypt(srcData, encryptCipherTransformer);
		logger.log(Level.INFO, "Encrypted Data (Round 1): "+StringUtil.toHex(encryptedData1));
		byte[] decryptedData1 = doDecrypt(encryptedData1, decryptCipherTransformer);
		
		byte[] encryptedData2 = doEncrypt(srcData, encryptCipherTransformer);
		byte[] decryptedData2 = doDecrypt(encryptedData2, decryptCipherTransformer);
		
		logger.log(Level.INFO, "Source Data:              "+StringUtil.toHex(srcData));
		logger.log(Level.INFO, "Decrypted Data (Round 1): "+StringUtil.toHex(decryptedData1));		
		logger.log(Level.INFO, "Decrypted Data (Round 2): "+StringUtil.toHex(decryptedData2));
		logger.log(Level.INFO, "Encrypted Data (Round 1): "+StringUtil.toHex(encryptedData1));
		logger.log(Level.INFO, "Encrypted Data (Round 2): "+StringUtil.toHex(encryptedData2));
		
		assertEquals("Source data and decrypted data is different (round 1)", StringUtil.toHex(srcData), StringUtil.toHex(decryptedData1));
		assertEquals("Source data and decrypted data is different (round 2)", StringUtil.toHex(srcData), StringUtil.toHex(decryptedData2));
		
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
	
	private SaltedSecretKey createDummyMasterKey() {
		return new SaltedSecretKey(
			new SecretKeySpec(
				StringUtil.fromHex("44fda24d53b29828b62c362529bd9df5c8a92c2736bcae3a28b3d7b44488e36e246106aa5334813028abb2048eeb5e177df1c702d93cf82aeb7b6d59a8534ff0"),
				"AnyAlgorithm"
			),
			StringUtil.fromHex("157599349e0f1bc713afff442db9d4c3201324073d51cb33407600f305500aa3fdb31136cb1f37bd51a48f183844257d42010a36133b32b424dd02bc63b349bc")			
		);
	}
}
