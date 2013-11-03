package org.syncany.crypto;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.syncany.util.FileUtil;

public class CipherUtil {
	private static final Logger logger = Logger.getLogger(CipherUtil.class.getSimpleName());
	
	public static final String PROVIDER = "BC";
    public static final String KEY_DERIVATION_FUNCTION = "PBKDF2WithHmacSHA1";
    public static final int KEY_DERIVATION_ROUNDS = 500;	    
    
    private static boolean initialized = false;
    private static boolean unlimitedStrengthEnabled = false;
    
    static {
		init();
    }
    
    public static synchronized void init() {
    	if (!initialized) {
    		logger.log(Level.INFO, "Initializing crypto settings and security provider ...");
    		
    		// Bouncy Castle
    		if (Security.getProvider(PROVIDER) == null) {
    			Security.addProvider(new BouncyCastleProvider()); 
    		}
    		
    		// Unlimited strength
    		try {
        		unlimitedStrengthEnabled = Cipher.getMaxAllowedKeyLength("AES") > 128;
        	}
        	catch (Exception e) {
        		unlimitedStrengthEnabled = false;
        	}
    		
    		initialized = true;
    	}    		
    }
    
    public static boolean unlimitedStrengthEnabled() {
    	return unlimitedStrengthEnabled;
    }
    
    public static void enableUnlimitedCrypto() throws CipherException {
    	if (!unlimitedStrengthEnabled) {
    		logger.log(Level.INFO, "Enabling unlimited strength/crypto ...");
    		
			try {
				Field field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
	
				field.setAccessible(true);
				field.set(null, false);		
			}
			catch (Exception e) {
				throw new CipherException(e);
			}
    	}
    }
    
	public static byte[] createRandomArray(int size) {
    	byte[] salt = new byte[size];    	
    	new SecureRandom().nextBytes(salt);
    	
    	return salt;
    }
	
	public static SecretKey createSecretKey(CipherSpec cipherSpec, String password, byte[] salt) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		return createSecretKey(cipherSpec.getCipherStr(), cipherSpec.getKeySize(), password, salt);
    }
	
	public static SecretKey createSecretKey(String algorithm, int keySize, String password, byte[] salt) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		logger.log(Level.INFO, "Creating secret key using "+KEY_DERIVATION_FUNCTION+" with "+KEY_DERIVATION_ROUNDS+" rounds ...");

		// Derive secret key from password 
    	SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_FUNCTION);
        KeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt, KEY_DERIVATION_ROUNDS, keySize);
        SecretKey secretKey = factory.generateSecret(pbeKeySpec);
        
        // The key name must be "AES" if cipherStr is "AES/...". This is really odd, but necessary
        String plainAlgorithm = (algorithm.indexOf('/') != -1) ? algorithm.substring(0, algorithm.indexOf('/')) : algorithm;
        SecretKey secretKeyAlgorithm = new SecretKeySpec(secretKey.getEncoded(), plainAlgorithm);  
        
        return secretKeyAlgorithm;
    }
	
	public static Cipher createCipher(CipherSpec cipherSpec, int cipherInitMode, SecretKey secretKey, byte[] iv) throws CipherException {
		logger.log(Level.INFO, "Creating cipher using "+cipherSpec+" ...");

		try {
			if (cipherSpec.needsUnlimitedStrength()) {
				CipherUtil.enableUnlimitedCrypto();
			}
			
            Cipher cipher = Cipher.getInstance(cipherSpec.getCipherStr(), PROVIDER);
            
            if (cipherSpec.hasIv()) {
            	cipher.init(cipherInitMode, secretKey, new IvParameterSpec(iv));
            }
            else {
            	cipher.init(cipherInitMode, secretKey);
            }

            return cipher;
        }
        catch (Exception e) {
            throw new CipherException(e);
        }
	}

	public static Cipher createEncCipher(CipherSpec cipherSuite, SecretKey secretKey, byte[] iv) throws CipherException {
		return createCipher(cipherSuite, Cipher.ENCRYPT_MODE, secretKey, iv);
	}   
	
	public static Cipher createDecCipher(CipherSpec cipherSuite, SecretKey secretKey, byte[] iv) throws CipherException {
		return createCipher(cipherSuite, Cipher.DECRYPT_MODE, secretKey, iv);
	}    	
	
	public static boolean isEncrypted(File file) throws IOException {
		byte[] actualMagic = new byte[MultiCipherOutputStream.STREAM_MAGIC.length];
		
		RandomAccessFile rFile = new RandomAccessFile(file, "r");
		rFile.read(actualMagic);
		rFile.close();
		
		return Arrays.equals(actualMagic, MultiCipherOutputStream.STREAM_MAGIC);
	}
	
	public static void encrypt(InputStream plaintextInputStream, OutputStream ciphertextOutputStream, List<CipherSpec> cipherSuites, String password) throws IOException {
		CipherSession cipherSession = new CipherSession(password);
		OutputStream multiCipherOutputStream = new MultiCipherOutputStream(ciphertextOutputStream, cipherSuites, cipherSession);
		
		FileUtil.appendToOutputStream(plaintextInputStream, multiCipherOutputStream);		
		
		multiCipherOutputStream.close();
		ciphertextOutputStream.close();
	}
	
	public static byte[] encrypt(InputStream plaintextInputStream, List<CipherSpec> cipherSuites, String password) throws IOException {
		ByteArrayOutputStream ciphertextOutputStream = new ByteArrayOutputStream();
		encrypt(plaintextInputStream, ciphertextOutputStream, cipherSuites, password);
		
		return ciphertextOutputStream.toByteArray();
	}
	
	public static byte[] encrypt(byte[] plaintext, List<CipherSpec> cipherSuites, String password) throws IOException {
		ByteArrayInputStream plaintextInputStream = new ByteArrayInputStream(plaintext);	
		ByteArrayOutputStream ciphertextOutputStream = new ByteArrayOutputStream();
		
		encrypt(plaintextInputStream, ciphertextOutputStream, cipherSuites, password);
		
		return ciphertextOutputStream.toByteArray();
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
