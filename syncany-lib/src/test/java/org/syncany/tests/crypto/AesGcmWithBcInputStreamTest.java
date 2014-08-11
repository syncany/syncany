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

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.io.InvalidCipherTextIOException;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Arrays;
import org.junit.Test;
import org.syncany.config.Logging;

/**
 * Tests to test the Bouncy Castle implementation of the CipherInputStream
 * 
 * @see https://github.com/binwiederhier/cipherinputstream-aes-gcm/blob/e9759ca71557e5d1da26ae72f6ce5aac918e34b0/src/CipherInputStreamIssuesTests.java
 * @see http://blog.philippheckel.com/2014/03/01/cipherinputstream-for-aead-modes-is-broken-in-jdk7-gcm/
 */
public class AesGcmWithBcInputStreamTest {
	private static final SecureRandom secureRandom = new SecureRandom();

	static {
		Logging.init();
		Security.addProvider(new BouncyCastleProvider());
	}
	
	@Test
	public void testD_BouncyCastleCipherInputStreamWithAesGcm() throws InvalidKeyException, InvalidAlgorithmParameterException, IOException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException {
		// Encrypt (not interesting in this example)
		byte[] randomKey = createRandomArray(16);
		byte[] randomIv = createRandomArray(16);		
		byte[] originalPlaintext = "Confirm 100$ pay".getBytes("ASCII"); 	
		byte[] originalCiphertext = encryptWithAesGcm(originalPlaintext, randomKey, randomIv);
		
		// Attack / alter ciphertext (an attacker would do this!) 
		byte[] alteredCiphertext = Arrays.clone(originalCiphertext);		
		alteredCiphertext[8] = (byte) (alteredCiphertext[8] ^ 0x08); // <<< Change 100$ to 900$
		
		// Decrypt with BouncyCastle implementation of CipherInputStream
		AEADBlockCipher cipher = new GCMBlockCipher(new AESEngine()); 
		cipher.init(false, new AEADParameters(new KeyParameter(randomKey), 128, randomIv));
		
		try {
			readFromStream(new org.bouncycastle.crypto.io.CipherInputStream(new ByteArrayInputStream(alteredCiphertext), cipher));
			//             ^^^^^^^^^^^^^^^ INTERESTING PART ^^^^^^^^^^^^^^^^	
			//
			//  The BouncyCastle implementation of the CipherInputStream detects MAC verification errors and
			//  throws a InvalidCipherTextIOException if an error occurs. Nice! A more or less minor issue
			//  however is that it is incompatible with the standard JCE Cipher class from the javax.crypto 
			//  package. The new interface AEADBlockCipher must be used. The code below is not executed.		

			fail("Test D: org.bouncycastle.crypto.io.CipherInputStream:        NOT OK, tampering not detected");						
		}
		catch (InvalidCipherTextIOException e) {
			System.out.println("Test D: org.bouncycastle.crypto.io.CipherInputStream:        OK, tampering detected");						
		}
	}
	
	@Test
	public void testE_BouncyCastleCipherInputStreamWithAesGcmLongPlaintext() throws InvalidKeyException, InvalidAlgorithmParameterException, IOException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException {
		// Encrypt (not interesting in this example)
		byte[] randomKey = createRandomArray(16);
		byte[] randomIv = createRandomArray(16);		
		byte[] originalPlaintext = createRandomArray(4080); // <<<< 4080 bytes fails, 4079 bytes works! 	
		byte[] originalCiphertext = encryptWithAesGcm(originalPlaintext, randomKey, randomIv);
		
		// Decrypt with BouncyCastle implementation of CipherInputStream
		AEADBlockCipher cipher = new GCMBlockCipher(new AESEngine()); 
		cipher.init(false, new AEADParameters(new KeyParameter(randomKey), 128, randomIv));
		
		try {
			readFromStream(new org.bouncycastle.crypto.io.CipherInputStream(new ByteArrayInputStream(originalCiphertext), cipher));
			//             ^^^^^^^^^^^^^^^ INTERESTING PART ^^^^^^^^^^^^^^^^	
			//
			//  In this example, the BouncyCastle implementation of the CipherInputStream throws an ArrayIndexOutOfBoundsException.
			//  The only difference to the example above is that the plaintext is now 4080 bytes long! For 4079 bytes plaintexts,
			//  everything works just fine.

			System.out.println("Test E: org.bouncycastle.crypto.io.CipherInputStream:        OK, throws no exception");						
		}
		catch (IOException e) {
			fail("Test E: org.bouncycastle.crypto.io.CipherInputStream:        NOT OK throws: "+e.getMessage());
		}
	}	
	
	private static byte[] readFromStream(InputStream inputStream) throws IOException {
		ByteArrayOutputStream decryptedPlaintextOutputStream = new ByteArrayOutputStream(); 
		
		int read = -1;
		byte[] buffer = new byte[16];
		
		while (-1 != (read = inputStream.read(buffer))) {
			decryptedPlaintextOutputStream.write(buffer, 0, read);
		}
		
		inputStream.close();
		decryptedPlaintextOutputStream.close();
		
		return decryptedPlaintextOutputStream.toByteArray();  		
	}
	
	private static byte[] encryptWithAesGcm(byte[] plaintext, byte[] randomKeyBytes, byte[] randomIvBytes) throws IOException, InvalidKeyException,
			InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException {

		SecretKey randomKey = new SecretKeySpec(randomKeyBytes, "AES");
		
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
		cipher.init(Cipher.ENCRYPT_MODE, randomKey, new IvParameterSpec(randomIvBytes));
		
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		CipherOutputStream cipherOutputStream = new CipherOutputStream(byteArrayOutputStream, cipher);
		
		cipherOutputStream.write(plaintext);
		cipherOutputStream.close();
		
		return byteArrayOutputStream.toByteArray();
	}
	
	private static byte[] createRandomArray(int size) {
		byte[] randomByteArray = new byte[size];
		secureRandom.nextBytes(randomByteArray);

		return randomByteArray;
	}
}
