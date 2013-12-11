/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;
import org.syncany.config.Logging;
import org.syncany.crypto.CipherParams;
import org.syncany.crypto.CipherSpec;
import org.syncany.crypto.CipherSpecs;
import org.syncany.crypto.CipherUtil;
import org.syncany.crypto.MultiCipherOutputStream;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class CipherUtilTest {
	private static final Logger logger = Logger.getLogger(CipherUtilTest.class.getSimpleName());
		
	static {
		Logging.init();
	}
	
	@Test
	public void testCreateMasterKeyWithSalt() throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		long timeStart = System.currentTimeMillis();
		
		SaltedSecretKey masterKeyForPasswordTestAndSalt123 = CipherUtil.createMasterKey("Test", new byte[] { 1, 2, 3 });
		
		long timeEnd = System.currentTimeMillis();
		long timeDuration = timeEnd - timeStart;

		logger.log(Level.INFO, "Creating master key took "+timeDuration+"ms:");
		logger.log(Level.INFO, " - Key:  "+StringUtil.toHex(masterKeyForPasswordTestAndSalt123.getEncoded()));
		logger.log(Level.INFO, " - Salt: "+StringUtil.toHex(masterKeyForPasswordTestAndSalt123.getSalt()));
						
		assertEquals("010203", StringUtil.toHex(masterKeyForPasswordTestAndSalt123.getSalt()));
		assertEquals("44fda24d53b29828b62c362529bd9df5c8a92c2736bcae3a28b3d7b44488e36e246106aa5334813028abb2048eeb5e177df1c702d93cf82aeb7b6d59a8534ff0",
			StringUtil.toHex(masterKeyForPasswordTestAndSalt123.getEncoded()));

		assertEquals(CipherParams.MASTER_KEY_SIZE/8, masterKeyForPasswordTestAndSalt123.getEncoded().length); 
		assertEquals("PBKDF2WithHmacSHA1", masterKeyForPasswordTestAndSalt123.getAlgorithm());
		assertEquals("RAW", masterKeyForPasswordTestAndSalt123.getFormat());
		 
		assertTrue(timeDuration > 5000);
	}
	
	@Test
	public void testCreateMasterKeyNoSalt() throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		SaltedSecretKey masterKeyForPasswordTestNoSalt1 = CipherUtil.createMasterKey("Test");
		SaltedSecretKey masterKeyForPasswordTestNoSalt2 = CipherUtil.createMasterKey("Test");
						
		logger.log(Level.INFO, "Key comparison for password 'Test':");
		logger.log(Level.INFO, "- Master key 1: "+StringUtil.toHex(masterKeyForPasswordTestNoSalt1.getEncoded()));
		logger.log(Level.INFO, "     with salt: "+StringUtil.toHex(masterKeyForPasswordTestNoSalt1.getSalt()));
		logger.log(Level.INFO, "- Master key 2: "+StringUtil.toHex(masterKeyForPasswordTestNoSalt2.getEncoded()));
		logger.log(Level.INFO, "     with salt: "+StringUtil.toHex(masterKeyForPasswordTestNoSalt2.getSalt()));
		
		assertFalse(Arrays.equals(masterKeyForPasswordTestNoSalt1.getSalt(), masterKeyForPasswordTestNoSalt2.getSalt()));
		assertFalse(Arrays.equals(masterKeyForPasswordTestNoSalt1.getEncoded(), masterKeyForPasswordTestNoSalt2.getEncoded()));
	}
	
	@Test
	public void testCreateDerivedKeys() throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		SaltedSecretKey masterKey = createDummyMasterKey();		
		CipherSpec cipherSpec = CipherSpecs.getCipherSpec(CipherSpecs.AES_128_GCM);
		
		byte[] derivedKeySalt1 = new byte[] { 1, 2, 3 };
		byte[] derivedKeySalt2 = new byte[] { 1, 2, 3, 4 };

		SaltedSecretKey derivedKey1 = CipherUtil.createDerivedKey(masterKey, derivedKeySalt1, cipherSpec);
		SaltedSecretKey derivedKey2 = CipherUtil.createDerivedKey(masterKey, derivedKeySalt2, cipherSpec);
		
		logger.log(Level.INFO, "- Derived key 1: "+StringUtil.toHex(derivedKey1.getEncoded()));
		logger.log(Level.INFO, "      with salt: "+StringUtil.toHex(derivedKey1.getSalt()));
		logger.log(Level.INFO, "- Derived key 2: "+StringUtil.toHex(derivedKey2.getEncoded()));
		logger.log(Level.INFO, "      with salt: "+StringUtil.toHex(derivedKey2.getSalt()));
		
		assertEquals(128/8, derivedKey1.getEncoded().length);
		assertEquals(128/8, derivedKey2.getEncoded().length);
		assertFalse(Arrays.equals(derivedKey1.getSalt(), derivedKey2.getSalt()));
		assertFalse(Arrays.equals(derivedKey1.getEncoded(), derivedKey2.getEncoded()));				
	}
	
	@Test
	public void testCreateRandomArray() throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		byte[] randomArray1 = CipherUtil.createRandomArray(10);
		byte[] randomArray2 = CipherUtil.createRandomArray(10);
		
		assertEquals(10, randomArray1.length);
		assertEquals(10, randomArray2.length);
		assertFalse(Arrays.equals(randomArray1, randomArray2));
	}
	
	@Test
	public void testIsEncryptedFileFalse() throws Exception {
		File tempDirectory = TestFileUtil.createTempDirectoryInSystemTemp();
		File testFile = new File(tempDirectory+"/somefile");
		
		FileUtil.writeToFile(new byte[] { 1,  2, 3 }, testFile);
		assertFalse(CipherUtil.isEncrypted(testFile));
		
		TestFileUtil.deleteDirectory(tempDirectory);
	}
	
	@Test
	public void testIsEncryptedFileTrue() throws Exception {
		File tempDirectory = TestFileUtil.createTempDirectoryInSystemTemp();
		File testFile = new File(tempDirectory+"/somefile");
		
		RandomAccessFile testFileRaf = new RandomAccessFile(testFile, "rw");
		
		testFileRaf.write(MultiCipherOutputStream.STREAM_MAGIC);
		testFileRaf.write(MultiCipherOutputStream.STREAM_VERSION);
		testFileRaf.close();
		
		assertTrue(CipherUtil.isEncrypted(testFile));
		
		TestFileUtil.deleteDirectory(tempDirectory);
	}
	
	@Test
	public void testEncryptShortArrayAes128Gcm() throws Exception {
		testEncrypt(
			new byte[] { 1,  2,  3, 4 },
			Arrays.asList(new CipherSpec[] { CipherSpecs.getCipherSpec(CipherSpecs.AES_128_GCM) })
		);		
	}
	
	@Test
	public void testEncryptLongArrayAes128Gcm() throws Exception {
		testEncrypt(
			TestFileUtil.createRandomArray(1024*1024),
			Arrays.asList(new CipherSpec[] { CipherSpecs.getCipherSpec(CipherSpecs.AES_128_GCM) })
		);				
	}
	
	@Test
	public void testEncryptShortArrayAes128Twofish128() throws Exception {
		testEncrypt(
			new byte[] { 1,  2,  3, 4 },
			Arrays.asList(new CipherSpec[] { 
				CipherSpecs.getCipherSpec(CipherSpecs.AES_128_GCM),
				CipherSpecs.getCipherSpec(CipherSpecs.TWOFISH_128_GCM)
			})
		);		
	}
	
	@Test
	public void testEncryptLongArrayAes128Twofish128() throws Exception {
		testEncrypt(
			TestFileUtil.createRandomArray(1024*1024),
			Arrays.asList(new CipherSpec[] { 
				CipherSpecs.getCipherSpec(CipherSpecs.AES_128_GCM),
				CipherSpecs.getCipherSpec(CipherSpecs.TWOFISH_128_GCM)
			})
		);		
	}
	
	@Test
	public void testEncryptLongArrayAes258Twofish256UnlimitedStrength() throws Exception {
		testEncrypt(
			TestFileUtil.createRandomArray(1024*1024),
			Arrays.asList(new CipherSpec[] { 
				CipherSpecs.getCipherSpec(CipherSpecs.AES_256_GCM),
				CipherSpecs.getCipherSpec(CipherSpecs.TWOFISH_256_GCM)
			})
		);		
	}
	
	@Test(expected=Exception.class)
	public void testEncryptUnknownCipherSpec() throws Exception {
		testEncrypt(
			TestFileUtil.createRandomArray(1024*1024),
			Arrays.asList(new CipherSpec[] { new CipherSpec(0xFF, "Twofish/GCM/NoPadding", 128, 128, false) })
		);		
	}
	
	private void testEncrypt(byte[] originalData, List<CipherSpec> cipherSpecs) throws IOException {
		SaltedSecretKey masterKey = createDummyMasterKey();
		
		byte[] ciphertext = CipherUtil.encrypt(
			new ByteArrayInputStream(originalData), 
			cipherSpecs,
			masterKey
		);
		
		byte[] plaintext = CipherUtil.decrypt(new ByteArrayInputStream(ciphertext), masterKey);
		
		assertFalse(Arrays.equals(originalData, ciphertext));
		assertTrue(Arrays.equals(originalData, plaintext));	
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
