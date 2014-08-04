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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

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
	
	/**
	 * Chars from A-Z / a-z to be used in randomly generated passwords. 
	 * 
	 * <p><b>Note:</b> This string cannot contain numbers, to prevent breaking
	 * of the vector clock format.
	 */
	private static final String ALPHABETIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	
	private static AtomicBoolean initialized = new AtomicBoolean(false);
	private static AtomicBoolean unlimitedStrengthEnabled = new AtomicBoolean(false);
	private static SecureRandom secureRandom = new SecureRandom();

	static {
		init();
	}

	/**
	 * Initializes the crypto provider ("Bouncy Castle") and tests whether the unlimited
	 * strength policy has been enabled. Unlimited crypto allows for stronger crypto algorithms
	 * such as AES-256 or Twofish-256.
	 * 
	 * <p>The method is called in the <tt>static</tt> block of this class and hence initialized
	 * whenever then class is used.
	 * 
	 * @see <a href="www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html">Java Cryptography Extension (JCE) Unlimited Strength</a> 
	 */
	public static synchronized void init() {
		if (!initialized.get()) {
			logger.log(Level.INFO, "Initializing crypto settings and security provider ...");

			// Bouncy Castle
			if (Security.getProvider(CRYPTO_PROVIDER_ID) == null) {
				Security.addProvider(CRYPTO_PROVIDER);
			}

			// Unlimited strength
			try {
				unlimitedStrengthEnabled.set(Cipher.getMaxAllowedKeyLength("AES") > 128);
			}
			catch (Exception e) {
				unlimitedStrengthEnabled.set(false);
			}

			initialized.set(true);
		}
	}

	/**
	 * Returns whether the unlimited strength policy is enabled in the current JVM.
	 * @see <a href="www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html">Java Cryptography
	 *      Extension (JCE) Unlimited Strength</a> 
	 */
	public static boolean unlimitedStrengthEnabled() {
		return unlimitedStrengthEnabled.get();
	}

	/**
	 * Attempts to programatically enable the unlimited strength Java crypto extension
	 * using the reflection API. 
	 * 
	 * <p>This class tries to set the property <tt>isRestricted</tt> of the class
	 * <tt>javax.crypto.JceSecurity</tt> to <tt>false</tt> -- effectively disabling 
	 * the artificial limitations (and the disallowed algorithms).
	 * 
	 * <p><b>Note</b>: Be aware that enabling the unlimited strength extension needs to 
	 * be acknowledged by the end-user to avoid legal issues!  
	 * 
	 * @throws CipherException If the unlimited strength policy cannot be enabled.
	 * @see <a href="www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html">Java Cryptography Extension (JCE) Unlimited Strength</a> 
	 */
	public static void enableUnlimitedStrength() throws CipherException {
		if (!unlimitedStrengthEnabled.get()) {
			logger.log(Level.FINE, "- Enabling unlimited strength/crypto ...");

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

	/**
	 * Creates a random array of bytes using the default {@link SecureRandom} implementation
	 * of the currently active JVM. The returned array can be used as a basis for secret keys,
	 * IVs or salts. 
	 * 
	 * @param size Size of the returned array (in bytes)
	 * @return Returns a random byte array (using a secure pseudo random generator) 
	 */
	public static byte[] createRandomArray(int size) {
		byte[] randomByteArray = new byte[size];
		secureRandom.nextBytes(randomByteArray);

		return randomByteArray;
	}
	
    /**
     * Generates a random string the given length. Only uses characters 
     * A-Z/a-z (in order to always create valid serialized vector clock representations).
     */
	public static String createRandomAlphabeticString(int size) {
		StringBuilder sb = new StringBuilder(size);
		
		for (int i = 0; i < size; i++) {
			sb.append(ALPHABETIC_CHARS.charAt(secureRandom.nextInt(ALPHABETIC_CHARS.length())));
		}
		
		return sb.toString();
	}

	/**
	 * Creates a derived key from the given {@link SecretKey} an input salt and wraps the key in 
	 * a {@link SecretKeySpec} using the given {@link CipherSpec}. 
	 * 
	 * <p>This method simply uses the {@link #createDerivedKey(byte[], byte[], String, int) createDerivedKey()}
	 * method using the encoded input key and the algorithm and key size given by the cipher spec.
	 * 
	 * @param inputKey The source key to derive the new key from
	 * @param inputSalt Input salt used to generate the new key (a non-secret random value!)
	 * @param outputCipherSpec Defines the algorithm and key size of the new output key
	 * @return Returns a derived key (including the given input salt)
	 */
	public static SaltedSecretKey createDerivedKey(SecretKey inputKey, byte[] inputSalt, CipherSpec outputCipherSpec)
			throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		
		return createDerivedKey(inputKey.getEncoded(), inputSalt, outputCipherSpec.getAlgorithm(), outputCipherSpec.getKeySize());
	}

	/**
	 * Creates a derived key from the given input key material (raw byte array) and an input salt
	 * and wraps the key in  a {@link SecretKeySpec} using the given output key algorithm and output
	 * key size.
	 * 
	 * <p>The algorithm used to derive the new key from the input key material (IKM) is the 
	 * <b>HMAC-based Extract-and-Expand Key Derivation Function (HKDF)</b> (see 
	 * <a href="http://tools.ietf.org/html/rfc5869">RFC 5869</a>)
	 * 
	 * @param inputKeyMaterial The input key material as raw data bytes, e.g. determined from {@link SecretKey#getEncoded()}
	 * @param inputSalt Input salt used to generate the new key (a non-secret random value!)
	 * @param outputKeyAlgorithm Defines the algorithm of the new output key (for {@link SecretKeySpec#getAlgorithm()})
	 * @param outputKeySize Defines the key size of the new output key 
	 * @return Returns a new pseudorandom key derived from the input key material using HKDF
	 * @see <a href="http://tools.ietf.org/html/rfc5869">RFC 5869</a>
	 */
	public static SaltedSecretKey createDerivedKey(byte[] inputKeyMaterial, byte[] inputSalt, String outputKeyAlgorithm, int outputKeySize)
			throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		
		HKDFBytesGenerator hkdf = new HKDFBytesGenerator(KEY_DERIVATION_DIGEST);
		hkdf.init(new HKDFParameters(inputKeyMaterial, inputSalt, KEY_DERIVATION_INFO));

		byte[] derivedKey = new byte[outputKeySize / 8];
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

	public static SaltedSecretKey createMasterKey(String password) throws CipherException {
		byte[] salt = createRandomArray(MASTER_KEY_SALT_SIZE / 8);
		return createMasterKey(password, salt);
	}

	public static SaltedSecretKey createMasterKey(String password, byte[] salt) throws CipherException {
		try {
			logger.log(Level.FINE, "- Creating secret key using {0} with {1} rounds, key size {2} bit ...", new Object[] { MASTER_KEY_DERIVATION_FUNCTION,
					MASTER_KEY_DERIVATION_ROUNDS, MASTER_KEY_SIZE });
	
			SecretKeyFactory factory = SecretKeyFactory.getInstance(MASTER_KEY_DERIVATION_FUNCTION);
			KeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt, MASTER_KEY_DERIVATION_ROUNDS, MASTER_KEY_SIZE);
			SecretKey masterKey = factory.generateSecret(pbeKeySpec);
	
			return new SaltedSecretKey(masterKey, salt);
		}
		catch (Exception e) {
			throw new CipherException(e);
		}
	}

	public static boolean isEncrypted(File file) throws IOException {
		byte[] actualMagic = new byte[MultiCipherOutputStream.STREAM_MAGIC.length];

		RandomAccessFile rFile = new RandomAccessFile(file, "r");
		rFile.read(actualMagic);
		rFile.close();

		return Arrays.equals(actualMagic, MultiCipherOutputStream.STREAM_MAGIC);
	}

	public static void encrypt(InputStream plaintextInputStream, OutputStream ciphertextOutputStream, List<CipherSpec> cipherSpecs,
			SaltedSecretKey masterKey) throws CipherException {

		try {
			CipherSession cipherSession = new CipherSession(masterKey);
			OutputStream multiCipherOutputStream = new MultiCipherOutputStream(ciphertextOutputStream, cipherSpecs, cipherSession);
	
			int read = -1;
			byte[] buffer = new byte[4096];
	
			while (-1 != (read = plaintextInputStream.read(buffer))) {
				multiCipherOutputStream.write(buffer, 0, read);
			}
			
			plaintextInputStream.close();
			multiCipherOutputStream.close();
		}
		catch (IOException e) {
			throw new CipherException(e);
		}
	}

	public static byte[] encrypt(InputStream plaintextInputStream, List<CipherSpec> cipherSuites, SaltedSecretKey masterKey) throws CipherException {
		ByteArrayOutputStream ciphertextOutputStream = new ByteArrayOutputStream();
		encrypt(plaintextInputStream, ciphertextOutputStream, cipherSuites, masterKey);

		return ciphertextOutputStream.toByteArray();
	}

	public static byte[] decrypt(InputStream fromInputStream, SaltedSecretKey masterKey) throws CipherException {
		try {
			CipherSession cipherSession = new CipherSession(masterKey);
			MultiCipherInputStream multiCipherInputStream = new MultiCipherInputStream(fromInputStream, cipherSession);
			ByteArrayOutputStream plaintextOutputStream = new ByteArrayOutputStream();
	
			int read = -1;
			byte[] buffer = new byte[4096];
	
			while (-1 != (read = multiCipherInputStream.read(buffer))) {
				plaintextOutputStream.write(buffer, 0, read);
			}
			
			multiCipherInputStream.close();
			plaintextOutputStream.close();
	
			return plaintextOutputStream.toByteArray();
		}
		catch (IOException e) {
			throw new CipherException(e);
		}
	}			
}
