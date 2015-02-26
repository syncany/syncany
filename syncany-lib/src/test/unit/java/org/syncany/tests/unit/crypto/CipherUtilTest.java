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
package org.syncany.tests.unit.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
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
import org.syncany.crypto.CipherException;
import org.syncany.crypto.CipherSpec;
import org.syncany.crypto.CipherSpecs;
import org.syncany.crypto.CipherUtil;
import org.syncany.crypto.MultiCipherOutputStream;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.tests.unit.util.TestFileUtil;
import org.syncany.util.StringUtil;

public class CipherUtilTest {
	private static final Logger logger = Logger.getLogger(CipherUtilTest.class.getSimpleName());
		
	static {
		Logging.init();
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
		
		TestFileUtil.writeToFile(new byte[] { 1,  2, 3 }, testFile);
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
	
	private void testEncrypt(byte[] originalData, List<CipherSpec> cipherSpecs) throws CipherException {
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
	
	@Test(expected = Exception.class)
	public void testIntegrityHeaderMagic() throws Exception {
		SaltedSecretKey masterKey = createDummyMasterKey();
		
		byte[] originalPlaintext = TestFileUtil.createRandomArray(50);
		
		byte[] ciphertext = CipherUtil.encrypt(
			new ByteArrayInputStream(originalPlaintext), 
			Arrays.asList(CipherSpecs.getCipherSpec(CipherSpecs.AES_128_GCM)),
			masterKey
		);
		
		// Alter header MAGIC BYTES 
		ciphertext[0] = 0x12;
		ciphertext[1] = 0x34;
		
		byte[] plaintext = CipherUtil.decrypt(new ByteArrayInputStream(ciphertext), masterKey);
		
		System.out.println(StringUtil.toHex(originalPlaintext));
		System.out.println(StringUtil.toHex(plaintext));
		
		fail("TEST FAILED: Ciphertext was altered without exception.");
	}	
	
	@Test(expected = Exception.class)
	public void testIntegrityHeaderVersion() throws Exception {
		SaltedSecretKey masterKey = createDummyMasterKey();
		
		byte[] originalPlaintext = TestFileUtil.createRandomArray(50);
		
		byte[] ciphertext = CipherUtil.encrypt(
			new ByteArrayInputStream(originalPlaintext), 
			Arrays.asList(CipherSpecs.getCipherSpec(CipherSpecs.AES_128_GCM)),
			masterKey
		);
		
		// Alter header VERSION 
		ciphertext[4] = (byte) 0xff;
		
		byte[] plaintext = CipherUtil.decrypt(new ByteArrayInputStream(ciphertext), masterKey);
		
		System.out.println(StringUtil.toHex(originalPlaintext));
		System.out.println(StringUtil.toHex(plaintext));
		
		fail("TEST FAILED: Ciphertext was altered without exception.");
	}	
	
	@Test(expected = Exception.class)
	public void testIntegrityHeaderCipherSpecId() throws Exception {
		SaltedSecretKey masterKey = createDummyMasterKey();
		
		byte[] originalPlaintext = TestFileUtil.createRandomArray(50);
		
		byte[] ciphertext = CipherUtil.encrypt(
			new ByteArrayInputStream(originalPlaintext), 
			Arrays.asList(CipherSpecs.getCipherSpec(CipherSpecs.AES_128_GCM)),
			masterKey
		);
		
		assertEquals(CipherSpecs.AES_128_GCM, ciphertext[18]); // If this fails, fix test!

		// Alter header CIPHER SPEC ID 		
		ciphertext[18] = (byte) 0xff;
		
		byte[] plaintext = CipherUtil.decrypt(new ByteArrayInputStream(ciphertext), masterKey);
		
		System.out.println(StringUtil.toHex(originalPlaintext));
		System.out.println(StringUtil.toHex(plaintext));
		
		fail("TEST FAILED: Ciphertext was altered without exception.");
	}	
	
	@Test(expected = Exception.class)
	public void testIntegrityHeaderCipherSalt() throws Exception {
		SaltedSecretKey masterKey = createDummyMasterKey();
		 
		byte[] originalPlaintext = TestFileUtil.createRandomArray(50);
		
		byte[] ciphertext = CipherUtil.encrypt(
			new ByteArrayInputStream(originalPlaintext), 
			Arrays.asList(CipherSpecs.getCipherSpec(CipherSpecs.AES_128_GCM)),
			masterKey
		);
		
		// Alter header CIPHER SALT 		
		ciphertext[19] = (byte) 0xff;
		ciphertext[20] = (byte) 0xff;
		ciphertext[21] = (byte) 0xff;
		
		byte[] plaintext = CipherUtil.decrypt(new ByteArrayInputStream(ciphertext), masterKey);
		
		System.out.println(StringUtil.toHex(originalPlaintext));
		System.out.println(StringUtil.toHex(plaintext));
		
		fail("TEST FAILED: Ciphertext was altered without exception.");
	}	
	
	@Test(expected = Exception.class)
	public void testIntegrityHeaderCipherIV() throws Exception {
		SaltedSecretKey masterKey = createDummyMasterKey();
		
		byte[] originalPlaintext = TestFileUtil.createRandomArray(50);
		
		byte[] ciphertext = CipherUtil.encrypt(
			new ByteArrayInputStream(originalPlaintext), 
			Arrays.asList(CipherSpecs.getCipherSpec(CipherSpecs.AES_128_GCM)),
			masterKey
		);
		
		// Alter header CIPHER SALT 		
		ciphertext[32] = (byte) 0xff;
		ciphertext[33] = (byte) 0xff;
		ciphertext[34] = (byte) 0xff;
		
		byte[] plaintext = CipherUtil.decrypt(new ByteArrayInputStream(ciphertext), masterKey);
		
		System.out.println(StringUtil.toHex(originalPlaintext));
		System.out.println(StringUtil.toHex(plaintext));
		
		fail("TEST FAILED: Ciphertext was altered without exception.");
	}	
	
	@Test(expected = CipherException.class)
	public void testIntegrityAesGcmCiphertext() throws Exception {
		SaltedSecretKey masterKey = createDummyMasterKey();
		
		byte[] originalPlaintext = TestFileUtil.createRandomArray(50);
		
		byte[] ciphertext = CipherUtil.encrypt(
			new ByteArrayInputStream(originalPlaintext), 
			Arrays.asList(CipherSpecs.getCipherSpec(CipherSpecs.AES_128_GCM)),
			masterKey
		);
		
		// Alter ciphertext (after header!); ciphertext starts after 75 bytes 
		ciphertext[80] = (byte) (ciphertext[80] ^ 0x01);
		ciphertext[81] = (byte) (ciphertext[81] ^ 0x02);
		ciphertext[82] = (byte) (ciphertext[82] ^ 0x03);
		
		CipherUtil.decrypt(new ByteArrayInputStream(ciphertext), masterKey);
		
		fail("TEST FAILED: Ciphertext was altered without exception.");
	}	
	
	@Test(expected = Exception.class)
	public void testIntegrityTwofishGcmCiphertext() throws Exception {
		SaltedSecretKey masterKey = createDummyMasterKey();
		
		byte[] originalPlaintext = TestFileUtil.createRandomArray(50);
		
		byte[] ciphertext = CipherUtil.encrypt(
			new ByteArrayInputStream(originalPlaintext), 
			Arrays.asList(CipherSpecs.getCipherSpec(CipherSpecs.TWOFISH_128_GCM)),
			masterKey
		);
		
		// Alter ciphertext (after header!); ciphertext starts after 75 bytes 
		ciphertext[80] = (byte) (ciphertext[80] ^ 0x01);
		
		byte[] plaintext = CipherUtil.decrypt(new ByteArrayInputStream(ciphertext), masterKey);
		
		System.out.println(StringUtil.toHex(originalPlaintext));
		System.out.println(StringUtil.toHex(plaintext));
		
		fail("TEST FAILED: Ciphertext was altered without exception.");
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
