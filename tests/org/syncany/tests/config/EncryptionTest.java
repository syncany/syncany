package org.syncany.tests.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.syncany.config.Encryption;
import org.syncany.util.StringUtil;


public class EncryptionTest {
	
	// Debug-Flag
	private boolean debug = true;
	
	private String pw = "password";
	private String salt = "saltsalt";
	private String text = "lorem ipsum dolor sit amet";
	
	@Test()
	public void unsaltedEncryptionTest() throws Exception {

		Encryption enc = new Encryption();
		enc.setPassword(pw);
		
		if(debug) System.out.println("encrypt:\t" + text);
		
		byte[] encrypted = enc.encrypt(text.getBytes());
		if(debug) System.out.println("encrypted:\t" + new String(encrypted));
		
		byte[] decrypted = enc.decrypt(encrypted);
		
		String decryptedText = new String(decrypted);
		if(debug) System.out.println("decrypted:\t" + decryptedText);
		
		Assert.assertEquals("text and decrypted text are not identical.", text, decryptedText);
		Assert.assertTrue("origin byte-array and decrypted byte-array are not identical.", Arrays.equals(decrypted, text.getBytes()));
	}
	
	
	@Test
	public void saltedEncryptionTest() throws Exception {
		if(debug) System.out.println("\nnow the same using salt");
		if(debug) System.out.println("encrypt:\t" + text);
		
		Encryption enc = new Encryption();
		enc.setPassword(pw);
		Encryption encSalted = new Encryption();
		encSalted.setPassword(pw);
		encSalted.setSalt(salt);
		
		byte[] encryptedSalted = encSalted.encrypt(text.getBytes());
		if(debug) System.out.println("encrypted:\t" + new String(encryptedSalted));
		byte[] encrypted = enc.encrypt(text.getBytes());
		if(debug) System.out.println("encrypted:\t" + new String(encrypted));
		
		Assert.assertFalse("Same byteData by encryption with salt as when without salt.", Arrays.equals(encryptedSalted, encrypted));
		
		byte[] decrypted = encSalted.decrypt(encryptedSalted);
		
		String decryptedText = new String(decrypted);
		if(debug) System.out.println("decrypted:\t" + decryptedText);
		
		Assert.assertEquals("Encrypted String and decrypted String are different!", text, decryptedText);
	}
	
	@Test
	public void byteStreamEncryptionTest() throws Exception {
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
		
		System.out.println("...");
		System.out.println("Encrypted Data: "+StringUtil.toHex(ENCRYPTED_DATA));
		System.out.println("Source Data:\t"+StringUtil.toHex(SOURCE_DATA));
		System.out.println("Decrypted Data:\t"+StringUtil.toHex(DECRYPTED_DATA));
		
		Assert.assertEquals("Encrypted and decrypted Data is different!", StringUtil.toHex(SOURCE_DATA), StringUtil.toHex(DECRYPTED_DATA));
	}

	// TODO: Test of encryption with metachunks with respect to consistency, but also to encryption-speed depending on file-size
	
}
