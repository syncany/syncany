package org.syncany;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;

import org.syncany.config.Encryption;
import org.syncany.util.StringUtil;

public class testabc {
	public static void main(String[] a) throws Exception {
		byte[] SOURCE_DATA = new byte[31];
		
		for (int i=0;i<SOURCE_DATA.length; i++) {
			SOURCE_DATA[i] = (byte)(i & 0xff);
		}
		
		byte[] key = ("SALT").getBytes("UTF-8");
		MessageDigest sha = MessageDigest.getInstance("SHA-1");
		key = sha.digest(key);
		key = Arrays.copyOf(key, 16);

		SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
		
		Cipher encC = Cipher.getInstance("AES/ECB/PKCS5Padding");
		encC.init(Cipher.ENCRYPT_MODE, secretKeySpec);
		
		Cipher decC = Cipher.getInstance("AES/ECB/PKCS5Padding");
		decC.init(Cipher.DECRYPT_MODE, secretKeySpec);
				
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		CipherOutputStream cos = new CipherOutputStream(bos, encC);
		DataOutputStream dos = new DataOutputStream(cos);
		
		dos.write(SOURCE_DATA, 0, SOURCE_DATA.length);
		dos.close();
		
		byte[] ENCRYPTED_DATA = bos.toByteArray();
		
		ByteArrayInputStream bis = new ByteArrayInputStream(ENCRYPTED_DATA);
		CipherInputStream cis = new CipherInputStream(bis, decC);
		DataInputStream dis = new DataInputStream(cis);
		
		
		/*int c = -1;
		while((c = dis.read()) != -1){
			System.out.println(c);
		}*/
		
		byte[] DECRYPTED_DATA = new byte[SOURCE_DATA.length];
		for(int i = 0; i < SOURCE_DATA.length; i++){
			DECRYPTED_DATA[i] = (byte)dis.read();
		}
		
		System.out.println("...");
		System.out.println("Source Data:\t\t"+ StringUtil.toHex(SOURCE_DATA));
		System.out.println("Encrypted Data:\t\t"+ StringUtil.toHex(ENCRYPTED_DATA));
		System.out.println("Decrypted Data:\t\t"+ StringUtil.toHex(DECRYPTED_DATA));
	}
}
