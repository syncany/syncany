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
	
	public static String decryptToString(InputStream inputStream, String password) throws IOException {
		CipherSession cipherSession = new CipherSession(password);
		MultiCipherInputStream cipherInputStream = new MultiCipherInputStream(inputStream, cipherSession);
		
		ByteArrayOutputStream plaintextOutputStream = new ByteArrayOutputStream();
					
		int read = -1;
		byte[] buffer = new byte[1024];
		
		while (-1 != (read = cipherInputStream.read(buffer))) {
			plaintextOutputStream.write(buffer, 0, read);
		}
		
		cipherInputStream.close();
		plaintextOutputStream.close();
		
		return new String(plaintextOutputStream.toByteArray());		
	}
	/*public static String decryptToString(InputStream inputStream, String password) throws IOException {
		String plaintextString = null;
		
		while (plaintextString == null) {
			CipherSession cipherSession = new CipherSession(password);
			AdvancedCipherInputStream cipherInputStream = new AdvancedCipherInputStream(inputStream, cipherSession);
			
			ByteArrayOutputStream plaintextOutputStream = new ByteArrayOutputStream();
						
			int read = -1;
			byte[] buffer = new byte[1024];
			
			while (-1 != (read = cipherInputStream.read(buffer))) {
				plaintextOutputStream.write(buffer, 0, read);
			}
			
			cipherInputStream.close();
			plaintextOutputStream.close();
			
			byte[] plaintextByteArray = plaintextOutputStream.toByteArray();
			
			if (plaintextByteArray.length >= AdvancedCipherInputStream.STREAM_MAGIC.length) { // TODO [medium] Workaround, this should be done with Adv(Adv(Adv(in))).read()
				byte[] plaintextPotentialMagic = new byte[AdvancedCipherInputStream.STREAM_MAGIC.length];
				System.arraycopy(plaintextByteArray, 0, plaintextPotentialMagic, 0, plaintextPotentialMagic.length);
				
				if (!Arrays.equals(AdvancedCipherInputStream.STREAM_MAGIC, plaintextPotentialMagic)) {
					plaintextString = new String(plaintextByteArray);
				}
				else {
					inputStream = new ByteArrayInputStream(plaintextByteArray);
				}
			}
			else {
				plaintextString = new String(plaintextByteArray);
			}
		}
		
		return plaintextString;		
	}*/
}
