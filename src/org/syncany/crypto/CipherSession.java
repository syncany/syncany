package org.syncany.crypto;

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

import org.syncany.config.Encryption;
import org.syncany.config.EncryptionException;

public class CipherSession {
	private CipherSuite cipherSuite;
	private String password;
	
	private byte[] sessionWriteSalt;
	private SecretKey sessionWriteSecretKey;
	
	private byte[] lastReadSalt;
	private SecretKey lastReadSecretKey;	
	
	public CipherSession(CipherSuite cipherSuite, String password) {
		this.cipherSuite = cipherSuite;
		this.password = password;
	}
	
    public CipherSuite getCipherSuite() {
		return cipherSuite;
	}

	public void setCipherSuite(CipherSuite cipherSuite) {
		this.cipherSuite = cipherSuite;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public byte[] getSessionWriteSalt() {
		return sessionWriteSalt;
	}

	public void setSessionWriteSalt(byte[] sessionWriteSalt) {
		this.sessionWriteSalt = sessionWriteSalt;
	}

	public SecretKey getSessionWriteSecretKey() {
		return sessionWriteSecretKey;
	}

	public void setSessionWriteSecretKey(SecretKey sessionWriteSecretKey) {
		this.sessionWriteSecretKey = sessionWriteSecretKey;
	}

	public byte[] getLastReadSalt() {
		return lastReadSalt;
	}

	public void setLastReadSalt(byte[] lastReadSalt) {
		this.lastReadSalt = lastReadSalt;
	}

	public SecretKey getLastReadSecretKey() {
		return lastReadSecretKey;
	}

	public void setLastReadSecretKey(SecretKey lastReadSecretKey) {
		this.lastReadSecretKey = lastReadSecretKey;
	}

	public byte[] createSalt() {
    	byte[] salt = new byte[cipherSuite.getKeySize()];    	
    	new SecureRandom().nextBytes(salt);
    	
    	return salt;
    }
    
    public SecretKey createSecretKey(byte[] keySalt) throws InvalidKeySpecException, NoSuchAlgorithmException {
    	// Derive secret key from password 
    	SecretKeyFactory factory = SecretKeyFactory.getInstance(Encryption.KEY_DERIVATION_FUNCTION);
        KeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), keySalt, 1000, cipherSuite.getKeySize());
        SecretKey secretKey = factory.generateSecret(pbeKeySpec);
        
        // The key name must be "AES" if cipherStr is "AES/...". This is really odd, but necessary
        String algorithm = (cipherSuite.getCipherStr().indexOf('/') != -1) ? cipherSuite.getCipherStr().substring(0, cipherSuite.getCipherStr().indexOf('/')) : cipherSuite.getCipherStr();
        SecretKey secretKeyAlgorithm = new SecretKeySpec(secretKey.getEncoded(), algorithm);  
        
        return secretKeyAlgorithm;
    }
    
	private Cipher createCipher(int cipherInitMode, SecretKey secretKey, byte[] iv) throws EncryptionException {
		try {
            Cipher cipher = Cipher.getInstance(cipherSuite.getCipherStr(), Encryption.PROVIDER);
            
            if (cipherSuite.isIv()) {
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

	public Cipher createEncCipher(SecretKey secretKey, byte[] iv) throws EncryptionException {
		return createCipher(Cipher.ENCRYPT_MODE, secretKey, iv);
	}   
	
	public Cipher createDecCipher(SecretKey secretKey, byte[] iv) throws EncryptionException {
		return createCipher(Cipher.DECRYPT_MODE, secretKey, iv);
	}    	
}
