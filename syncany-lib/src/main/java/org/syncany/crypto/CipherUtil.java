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
package org.syncany.crypto;

import static org.syncany.crypto.CipherParams.CRYPTO_PROVIDER;
import static org.syncany.crypto.CipherParams.CRYPTO_PROVIDER_ID;
import static org.syncany.crypto.CipherParams.KEY_DERIVATION_DIGEST;
import static org.syncany.crypto.CipherParams.KEY_DERIVATION_INFO;
import static org.syncany.crypto.CipherParams.MASTER_KEY_DERIVATION_FUNCTION;
import static org.syncany.crypto.CipherParams.MASTER_KEY_DERIVATION_ROUNDS;
import static org.syncany.crypto.CipherParams.MASTER_KEY_SALT_SIZE;
import static org.syncany.crypto.CipherParams.MASTER_KEY_SIZE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.syncany.util.FileUtil;

/**
 * The cipher utility provides functions to create a master key using PBKDF2, 
 * a derived key using SHA256, and to create a {@link Cipher} from a derived key.
 * It furthermore offers a method to programmatically enable the unlimited strength
 * crypto policies. 
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class CipherUtil {
	private static final Logger logger = Logger.getLogger(CipherUtil.class.getSimpleName());

    private static boolean initialized = false;
    private static boolean unlimitedStrengthEnabled = false;
    private static SecureRandom secureRandom = new SecureRandom();
    
    static {
		init();
    }
    
    public static synchronized void init() {
    	if (!initialized) {
    		logger.log(Level.INFO, "Initializing crypto settings and security provider ...");
    		
    		// Bouncy Castle
    		if (Security.getProvider(CRYPTO_PROVIDER_ID) == null) {
    			Security.addProvider(CRYPTO_PROVIDER); 
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
    
    public static void enableUnlimitedStrength() throws CipherException {
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
    	secureRandom.nextBytes(salt);
    	
    	return salt;
    }
	
	public static SaltedSecretKey createDerivedKey(SecretKey inputMasterKey, byte[] inputSalt, CipherSpec outputCipherSpec) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		return createDerivedKey(inputMasterKey.getEncoded(), inputSalt, outputCipherSpec.getAlgorithm(), outputCipherSpec.getKeySize());
    }
	
	public static SaltedSecretKey createDerivedKey(byte[] inputKeyMaterial, byte[] inputSalt, String outputKeyAlgorithm, int outputKeySize) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		logger.log(Level.INFO, "Creating secret key using ...");		
		
		HKDFBytesGenerator hkdf = new HKDFBytesGenerator(KEY_DERIVATION_DIGEST);
		hkdf.init(new HKDFParameters(inputKeyMaterial, inputSalt, KEY_DERIVATION_INFO));
		
		byte[] derivedKey = new byte[outputKeySize/8];
		hkdf.generateBytes(derivedKey, 0, derivedKey.length);
		
        return toSaltedSecretKey(derivedKey, inputSalt, outputKeyAlgorithm);
    }
	
	public static SecretKey toSecretKey(byte[] secretKeyBytes, String algorithm) {
		String plainAlgorithm = (algorithm.indexOf('/') != -1) ? algorithm.substring(0, algorithm.indexOf('/')) : algorithm;
        SecretKey secretKey = new SecretKeySpec(secretKeyBytes, plainAlgorithm);  
        
        return secretKey;
	}
	
	public static SaltedSecretKey toSaltedSecretKey(byte[] secretKeyBytes, byte[] saltBytes, String algorithm) {
		return new SaltedSecretKey(toSecretKey(secretKeyBytes, algorithm), saltBytes);
	}
	
	public static SaltedSecretKey createMasterKey(String password) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		byte[] salt = createRandomArray(MASTER_KEY_SALT_SIZE/8);
		return createMasterKey(password, salt);
    }

	public static SaltedSecretKey createMasterKey(String password, byte[] salt) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		logger.log(Level.INFO, "Creating secret key using "+MASTER_KEY_DERIVATION_FUNCTION+" with "+MASTER_KEY_DERIVATION_ROUNDS+" rounds, key size "+MASTER_KEY_SIZE+" bit ...");
		
    	SecretKeyFactory factory = SecretKeyFactory.getInstance(MASTER_KEY_DERIVATION_FUNCTION);
        KeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt, MASTER_KEY_DERIVATION_ROUNDS, MASTER_KEY_SIZE);
        SecretKey masterKey = factory.generateSecret(pbeKeySpec);
        
        return new SaltedSecretKey(masterKey, salt);
    }
		
	public static Cipher createCipher(CipherSpec cipherSpec, int cipherInitMode, SecretKey secretKey, byte[] iv) throws CipherException {
		logger.log(Level.INFO, "Creating cipher using "+cipherSpec+" ...");

		try {
			if (cipherSpec.needsUnlimitedStrength()) {
				enableUnlimitedStrength();
			}
			
            Cipher cipher = Cipher.getInstance(cipherSpec.getAlgorithm(), CRYPTO_PROVIDER_ID);
        	cipher.init(cipherInitMode, secretKey, new IvParameterSpec(iv));

            return cipher;
        }
        catch (Exception e) {
            throw new CipherException(e);
        }
	}

	public static Cipher createEncCipher(CipherSpec cipherSpec, SecretKey secretKey, byte[] iv) throws CipherException {
		return createCipher(cipherSpec, Cipher.ENCRYPT_MODE, secretKey, iv);
	}   
	
	public static Cipher createDecCipher(CipherSpec cipherSpec, SecretKey secretKey, byte[] iv) throws CipherException {
		return createCipher(cipherSpec, Cipher.DECRYPT_MODE, secretKey, iv);
	}    	
	
	public static boolean isEncrypted(File file) throws IOException {
		byte[] actualMagic = new byte[MultiCipherOutputStream.STREAM_MAGIC.length];
		
		RandomAccessFile rFile = new RandomAccessFile(file, "r");
		rFile.read(actualMagic);
		rFile.close();
		
		return Arrays.equals(actualMagic, MultiCipherOutputStream.STREAM_MAGIC);
	}
	
	public static void encrypt(InputStream plaintextInputStream, OutputStream ciphertextOutputStream, List<CipherSpec> cipherSuites, SaltedSecretKey masterKey) throws IOException {
		CipherSession cipherSession = new CipherSession(masterKey);
		OutputStream multiCipherOutputStream = new MultiCipherOutputStream(ciphertextOutputStream, cipherSuites, cipherSession);
		
		FileUtil.appendToOutputStream(plaintextInputStream, multiCipherOutputStream);		
		
		multiCipherOutputStream.close();
		ciphertextOutputStream.close();
	}
	
	public static byte[] encrypt(InputStream plaintextInputStream, List<CipherSpec> cipherSuites, SaltedSecretKey masterKey) throws IOException {
		ByteArrayOutputStream ciphertextOutputStream = new ByteArrayOutputStream();
		encrypt(plaintextInputStream, ciphertextOutputStream, cipherSuites, masterKey);
		
		return ciphertextOutputStream.toByteArray();
	}
	
	public static byte[] encrypt(byte[] plaintext, List<CipherSpec> cipherSuites, SaltedSecretKey masterKey) throws IOException {
		ByteArrayInputStream plaintextInputStream = new ByteArrayInputStream(plaintext);	
		ByteArrayOutputStream ciphertextOutputStream = new ByteArrayOutputStream();
		
		encrypt(plaintextInputStream, ciphertextOutputStream, cipherSuites, masterKey);
		
		return ciphertextOutputStream.toByteArray();
	}
	
	public static byte[] decrypt(InputStream fromInputStream, SaltedSecretKey masterKey) throws IOException {		
		CipherSession cipherSession = new CipherSession(masterKey);
		MultiCipherInputStream cipherInputStream = new MultiCipherInputStream(fromInputStream, cipherSession);
		ByteArrayOutputStream plaintextOutputStream = new ByteArrayOutputStream();
		
		FileUtil.appendToOutputStream(cipherInputStream, plaintextOutputStream);
					
		cipherInputStream.close();
		plaintextOutputStream.close();
		
		return plaintextOutputStream.toByteArray();		
	}	
}
