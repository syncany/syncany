package org.syncany.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.syncany.config.Encryption;
import org.syncany.config.EncryptionException;
import org.syncany.util.FileUtil;

public class CipherUtil {
	public static byte[] createRandomArray(int size) {
    	byte[] salt = new byte[size];    	
    	new SecureRandom().nextBytes(salt);
    	
    	return salt;
    }
	
	public static SecretKey createSecretKey(CipherSuite cipherSuite, String password, byte[] salt) throws InvalidKeySpecException, NoSuchAlgorithmException {
    	// Derive secret key from password 
    	SecretKeyFactory factory = SecretKeyFactory.getInstance(Encryption.KEY_DERIVATION_FUNCTION);
        KeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt, Encryption.KEY_DERIVATION_ROUNDS, cipherSuite.getKeySize());
        SecretKey secretKey = factory.generateSecret(pbeKeySpec);
        
        // The key name must be "AES" if cipherStr is "AES/...". This is really odd, but necessary
        String algorithm = (cipherSuite.getCipherStr().indexOf('/') != -1) ? cipherSuite.getCipherStr().substring(0, cipherSuite.getCipherStr().indexOf('/')) : cipherSuite.getCipherStr();
        SecretKey secretKeyAlgorithm = new SecretKeySpec(secretKey.getEncoded(), algorithm);  
        
        return secretKeyAlgorithm;
    }
	
	public static Cipher createCipher(CipherSuite cipherSuite, int cipherInitMode, SecretKey secretKey, byte[] iv) throws EncryptionException {
		try {
            Cipher cipher = Cipher.getInstance(cipherSuite.getCipherStr(), Encryption.PROVIDER);
            
            if (cipherSuite.hasIv()) {
            	cipher.init(cipherInitMode, secretKey, new IvParameterSpec(iv));
            }
            else {
            	cipher.init(cipherInitMode, secretKey);
            }

            return cipher;
        }
        catch (Exception e) {
            throw new EncryptionException(e);
        }
	}

	public static Cipher createEncCipher(CipherSuite cipherSuite, SecretKey secretKey, byte[] iv) throws EncryptionException {
		return createCipher(cipherSuite, Cipher.ENCRYPT_MODE, secretKey, iv);
	}   
	
	public static Cipher createDecCipher(CipherSuite cipherSuite, SecretKey secretKey, byte[] iv) throws EncryptionException {
		return createCipher(cipherSuite, Cipher.DECRYPT_MODE, secretKey, iv);
	}    	
	
	public static String decryptToString(InputStream fromInputStream, String password) throws IOException {
		return new String(decrypt(fromInputStream, password));
	}
	
	public static byte[] decrypt(InputStream fromInputStream, String password) throws IOException {		
		CipherSession cipherSession = new CipherSession(password);
		MultiCipherInputStream cipherInputStream = new MultiCipherInputStream(fromInputStream, cipherSession);
		ByteArrayOutputStream plaintextOutputStream = new ByteArrayOutputStream();
		
		FileUtil.appendToOutputStream(cipherInputStream, plaintextOutputStream);
					
		cipherInputStream.close();
		plaintextOutputStream.close();
		
		return plaintextOutputStream.toByteArray();		
	}	
}
