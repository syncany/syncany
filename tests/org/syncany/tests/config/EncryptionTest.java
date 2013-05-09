package org.syncany.tests.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import org.junit.Assert;
import org.junit.Test;
import org.syncany.config.Encryption;
import org.syncany.util.StringUtil;


public class EncryptionTest {
	private static final Logger logger = Logger.getLogger(EncryptionTest.class.getSimpleName());
	
	private String pw = "password";
	private String salt = "saltsalt";
	private String text = "lorem ipsum dolor sit amet";
	
	@Test()
	public void testUnsaltedEncryption() throws Exception {
		Encryption enc = new Encryption();
		enc.setPassword(pw);
		
		logger.log(Level.INFO, "encrypt:\t" + text);
		
		byte[] encrypted = enc.encrypt(text.getBytes());
		logger.log(Level.INFO, "encrypted:\t" + new String(encrypted));
		
		byte[] decrypted = enc.decrypt(encrypted);
		
		String decryptedText = new String(decrypted);
		logger.log(Level.INFO, "decrypted:\t" + decryptedText);
		
		Assert.assertEquals("text and decrypted text are not identical.", text, decryptedText);
		Assert.assertTrue("origin byte-array and decrypted byte-array are not identical.", Arrays.equals(decrypted, text.getBytes()));
	}
	
	
	@Test
	public void testSaltedEncryption() throws Exception {
		logger.log(Level.INFO, "\nnow the same using salt");
		logger.log(Level.INFO, "encrypt:\t" + text);
		
		Encryption enc = new Encryption();
		enc.setPassword(pw);
		Encryption encSalted = new Encryption();
		encSalted.setPassword(pw);
		encSalted.setSalt(salt);
		
		byte[] encryptedSalted = encSalted.encrypt(text.getBytes());
		logger.log(Level.INFO, "encrypted:\t" + new String(encryptedSalted));
		byte[] encrypted = enc.encrypt(text.getBytes());
		logger.log(Level.INFO, "encrypted:\t" + new String(encrypted));
		
		Assert.assertFalse("Same byteData by encryption with salt as when without salt.", Arrays.equals(encryptedSalted, encrypted));
		
		byte[] decrypted = encSalted.decrypt(encryptedSalted);
		
		String decryptedText = new String(decrypted);
		logger.log(Level.INFO, "decrypted:\t" + decryptedText);
		
		Assert.assertEquals("Encrypted String and decrypted String are different!", text, decryptedText);
	}
	
	@Test
	public void testByteStreamEncryption() throws Exception {
		byte[] SOURCE_DATA = new byte[64];
		
		for (int i=0;i<SOURCE_DATA.length; i++) {
			SOURCE_DATA[i] = (byte)(i & 0xff);
		}
		
		Encryption e = new Encryption();
		e.setPassword("pw");
		e.setSalt("SALT");
		Cipher encC = e.getEncCipher();
		Cipher decC = e.getDecCipher();
				
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		CipherOutputStream cos = new CipherOutputStream(bos, encC);
		DataOutputStream dos = new DataOutputStream(cos);
		
		dos.write(SOURCE_DATA, 0, SOURCE_DATA.length);
		dos.close();
		
		byte[] ENCRYPTED_DATA = bos.toByteArray();
		
		ByteArrayInputStream bis = new ByteArrayInputStream(ENCRYPTED_DATA);
		CipherInputStream cis = new CipherInputStream(bis, decC);
		DataInputStream dis = new DataInputStream(cis);
		
		byte[] DECRYPTED_DATA = new byte[SOURCE_DATA.length];
		
		for (int i=0;i<SOURCE_DATA.length; i++) {
			dis.read(DECRYPTED_DATA);
		}
		dis.read(DECRYPTED_DATA, 0, DECRYPTED_DATA.length);
		
		logger.log(Level.INFO, "Encrypted Data: "+StringUtil.toHex(ENCRYPTED_DATA));
		logger.log(Level.INFO, "Source Data:\t"+StringUtil.toHex(SOURCE_DATA));
		logger.log(Level.INFO, "Decrypted Data:\t"+StringUtil.toHex(DECRYPTED_DATA));
		
		Assert.assertEquals("Encrypted and decrypted Data is different!", StringUtil.toHex(SOURCE_DATA), StringUtil.toHex(DECRYPTED_DATA));
	}

	// TODO: Test of encryption with metachunks with respect to consistency, but also to encryption-speed depending on file-size
	
}
