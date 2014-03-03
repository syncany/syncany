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

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.Cipher;
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
import org.junit.Test;
import org.syncany.crypto.CipherUtil;
import org.syncany.util.StringUtil;

public class GcmCipherInputStreamTest {
	@Test(expected = Exception.class)
	public void testGcmCipherInputStream() throws InvalidKeyException, InvalidAlgorithmParameterException, IOException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException {
		byte[] originalPlaintext = StringUtil.fromHex("00000000000000000000000000000000");
		
		// Encrypt with CipherOutputStream
		SecretKey randomKey = new SecretKeySpec(CipherUtil.createRandomArray(16), "AES");
		byte[] randomIv = CipherUtil.createRandomArray(16);
		
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
		cipher.init(Cipher.ENCRYPT_MODE, randomKey, new IvParameterSpec(randomIv));
		
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		javax.crypto.CipherOutputStream cipherOutputStream = new javax.crypto.CipherOutputStream(byteArrayOutputStream, cipher);
		
		cipherOutputStream.write(originalPlaintext);
		cipherOutputStream.close();
		
		byte[] originalCiphertext = byteArrayOutputStream.toByteArray();
		
		// Alter ciphertext 
		byte[] alteredCiphertext = new byte[originalCiphertext.length];
		System.arraycopy(originalCiphertext, 0, alteredCiphertext, 0, originalCiphertext.length);
		
		alteredCiphertext[10] = (byte) (alteredCiphertext[10] ^ 0x01);
		alteredCiphertext[11] = (byte) (alteredCiphertext[11] ^ 0x02);
		
		// Decrypt with CipherInputStream
		cipher.init(Cipher.DECRYPT_MODE, randomKey, new IvParameterSpec(randomIv));
		
		ByteArrayOutputStream decryptedPlaintextOutputStream = new ByteArrayOutputStream(); 
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(alteredCiphertext);
		javax.crypto.CipherInputStream cipherInputStream = new javax.crypto.CipherInputStream(byteArrayInputStream, cipher);
		
		int read = -1;
		byte[] buffer = new byte[16];
		
		while (-1 != (read = cipherInputStream.read(buffer))) {
			decryptedPlaintextOutputStream.write(buffer, 0, read);
		}
		
		cipherInputStream.close();
		decryptedPlaintextOutputStream.close();
		
		byte[] decryptedPlaintext = decryptedPlaintextOutputStream.toByteArray();  
		
		System.out.println("Original plaintext:  "+StringUtil.toHex(originalPlaintext));
		System.out.println("Decrypted plaintext: "+StringUtil.toHex(decryptedPlaintext));
		System.out.println("Original ciphertext: "+StringUtil.toHex(originalCiphertext));
		System.out.println("Altered ciphertext:  "+StringUtil.toHex(alteredCiphertext));
		
		fail("TEST FAILED: Ciphertext was altered without exception.");
	}
	
	@Test(expected = InvalidCipherTextIOException.class)
	public void testBouncyCastleGcmCipherInputStream() throws InvalidKeyException, InvalidAlgorithmParameterException, IOException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException {
		byte[] originalPlaintext = StringUtil.fromHex("00000000000000000000000000000000");
		
		// Encrypt with CipherOutputStream
		AEADBlockCipher cipher = new GCMBlockCipher(new AESEngine()); 
		KeyParameter secretKey = new KeyParameter(CipherUtil.createRandomArray(16));
		byte[] randomIv = CipherUtil.createRandomArray(16);
		
		cipher.init(true, new AEADParameters(secretKey, 128, randomIv));
		
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		org.bouncycastle.crypto.io.CipherOutputStream cipherOutputStream = new org.bouncycastle.crypto.io.CipherOutputStream(byteArrayOutputStream, cipher);
		
		cipherOutputStream.write(originalPlaintext);
		cipherOutputStream.close();
		
		byte[] originalCiphertext = byteArrayOutputStream.toByteArray();
		
		// Alter ciphertext 
		byte[] alteredCiphertext = new byte[originalCiphertext.length];
		System.arraycopy(originalCiphertext, 0, alteredCiphertext, 0, originalCiphertext.length);
		
		alteredCiphertext[10] = (byte) (alteredCiphertext[10] ^ 0x01);
		alteredCiphertext[11] = (byte) (alteredCiphertext[11] ^ 0x02);
		
		// Decrypt with CipherInputStream
		cipher.init(false, new AEADParameters(secretKey, 128, randomIv));
		
		ByteArrayOutputStream decryptedPlaintextOutputStream = new ByteArrayOutputStream(); 
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(alteredCiphertext);
		org.bouncycastle.crypto.io.CipherInputStream cipherInputStream = new org.bouncycastle.crypto.io.CipherInputStream(byteArrayInputStream, cipher);
		
		int read = -1;
		byte[] buffer = new byte[16];
		
		while (-1 != (read = cipherInputStream.read(buffer))) {
			decryptedPlaintextOutputStream.write(buffer, 0, read);
		}
		
		cipherInputStream.close();
		decryptedPlaintextOutputStream.close();
		
		byte[] decryptedPlaintext = decryptedPlaintextOutputStream.toByteArray();  
		
		System.out.println("Original plaintext:  "+StringUtil.toHex(originalPlaintext));
		System.out.println("Decrypted plaintext: "+StringUtil.toHex(decryptedPlaintext));
		System.out.println("Original ciphertext: "+StringUtil.toHex(originalCiphertext));
		System.out.println("Altered ciphertext:  "+StringUtil.toHex(alteredCiphertext));
		
		fail("TEST FAILED: Ciphertext was altered without exception.");
	}
}
