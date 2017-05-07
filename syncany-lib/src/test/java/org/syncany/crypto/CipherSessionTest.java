/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;
import org.syncany.config.Logging;
import org.syncany.util.StringUtil;

public class CipherSessionTest {
	static {
		Logging.init();
	}
	
	@Test
	public void testCipherSessionWriteKeyReuseCountOfTwo() throws Exception {
		SaltedSecretKey masterKey = createDummyMasterKey();		
		CipherSession cipherSession = new CipherSession(masterKey, 999, 2);
		
		CipherSpec cipherSpecAes128 = CipherSpecs.getCipherSpec(CipherSpecs.AES_128_GCM);
		CipherSpec cipherSpecTwofish128 = CipherSpecs.getCipherSpec(CipherSpecs.TWOFISH_128_GCM);
		
		SaltedSecretKey writeSecretKey1Aes128 = cipherSession.getWriteSecretKey(cipherSpecAes128);
		SaltedSecretKey writeSecretKey2Aes128 = cipherSession.getWriteSecretKey(cipherSpecAes128);
		SaltedSecretKey writeSecretKey3Aes128 = cipherSession.getWriteSecretKey(cipherSpecAes128);
		
		SaltedSecretKey writeSecretKey1Twofish128 = cipherSession.getWriteSecretKey(cipherSpecTwofish128);
		SaltedSecretKey writeSecretKey2Twofish128 = cipherSession.getWriteSecretKey(cipherSpecTwofish128);
		SaltedSecretKey writeSecretKey3Twofish128 = cipherSession.getWriteSecretKey(cipherSpecTwofish128);
				
		assertEquals(writeSecretKey1Aes128, writeSecretKey2Aes128);
		assertNotSame(writeSecretKey1Aes128, writeSecretKey3Aes128);
		
		assertEquals(writeSecretKey1Twofish128, writeSecretKey2Twofish128);
		assertNotSame(writeSecretKey1Twofish128, writeSecretKey3Twofish128);
		
		assertNotSame(writeSecretKey1Aes128, writeSecretKey1Twofish128);
	}	
	
	@Test
	public void testCipherSessionReadKeyCacheSizeOfThree() throws Exception {
		SaltedSecretKey masterKey = createDummyMasterKey();		
		CipherSession cipherSession = new CipherSession(masterKey, 2, 999);
		
		CipherSpec cipherSpecAes128 = CipherSpecs.getCipherSpec(CipherSpecs.AES_128_GCM);
		
		byte[] readKeySalt1 = CipherUtil.createRandomArray(cipherSpecAes128.getKeySize());
		byte[] readKeySalt2 = CipherUtil.createRandomArray(cipherSpecAes128.getKeySize());
		byte[] readKeySalt3 = CipherUtil.createRandomArray(cipherSpecAes128.getKeySize());				
		
		SaltedSecretKey readSecretKey1Aes128 = cipherSession.getReadSecretKey(cipherSpecAes128, readKeySalt1);
		SaltedSecretKey readSecretKey2Aes128 = cipherSession.getReadSecretKey(cipherSpecAes128, readKeySalt2);
		SaltedSecretKey readSecretKey3Aes128 = cipherSession.getReadSecretKey(cipherSpecAes128, readKeySalt3);
		
		assertNotSame(readSecretKey1Aes128, readSecretKey2Aes128);
		assertNotSame(readSecretKey1Aes128, readSecretKey3Aes128);
		assertNotSame(readSecretKey2Aes128, readSecretKey3Aes128);
		
		// TODO [medium] This does NOT TEST the actual read cache. How to test this. The cache is completely hidden/private?!
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
