package org.syncany.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.SecretKey;

import org.syncany.util.StringUtil;

public class CipherSession {
    private static final Logger logger = Logger.getLogger(CipherSession.class.getSimpleName());   
	private static final int DEFAULT_SECRET_KEY_READ_CACHE_SIZE = 10;
	private static final int DEFAULT_SECRET_KEY_WRITE_REUSE_COUNT = 100;
	
	private String password;	
	
	private Map<CipherSpecWithSalt, SecretKeyCacheEntry> secretKeyReadCache;
	private int secretKeyReadCacheSize;
	
	private Map<CipherSpec, SecretKeyCacheEntry> secretKeyWriteCache;
	private int secretKeyWriteReuseCount;
	
	public CipherSession(String password) {
		this(password, DEFAULT_SECRET_KEY_READ_CACHE_SIZE, DEFAULT_SECRET_KEY_WRITE_REUSE_COUNT);
	}
	
	public CipherSession(String password, int secretKeyReadCacheSize, int secretKeyWriteReuseCount) {
		this.password = password;

		this.secretKeyReadCache = new LinkedHashMap<CipherSpecWithSalt, SecretKeyCacheEntry>();
		this.secretKeyReadCacheSize = secretKeyReadCacheSize;
		
		this.secretKeyWriteCache = new HashMap<CipherSpec, SecretKeyCacheEntry>();
		this.secretKeyWriteReuseCount = secretKeyWriteReuseCount;
	}	
	
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}	

	public SecretKeyCacheEntry getWriteSecretKeyCacheEntry(CipherSpec cipherSpec) {
		SecretKeyCacheEntry secretKeyCacheEntry = secretKeyWriteCache.get(cipherSpec);
		
		if (secretKeyCacheEntry != null && secretKeyCacheEntry.getUseCount() >= secretKeyWriteReuseCount) {
			secretKeyWriteCache.remove(cipherSpec);
			secretKeyCacheEntry = null;
		}
		
		return secretKeyCacheEntry;
	}
	
	public void putWriteSecretKeyCacheEntry(CipherSpec cipherSpec, SecretKeyCacheEntry secretKeyCacheEntry) {
		secretKeyWriteCache.put(cipherSpec, secretKeyCacheEntry);
	}
	
	public SecretKey getReadSecretKey(CipherSpec cipherSpec, byte[] salt) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		CipherSpecWithSalt cipherSpecWithSalt = new CipherSpecWithSalt(cipherSpec, salt);
		SecretKeyCacheEntry secretKeyCacheEntry = secretKeyReadCache.get(cipherSpecWithSalt);
		
		if (secretKeyCacheEntry != null) {
			logger.log(Level.INFO, "Using cached secret key, with salt "+StringUtil.toHex(salt));
			return secretKeyCacheEntry.getSecretKey();
		}
		else {
			SecretKey secretKey = CipherUtil.createSecretKey(cipherSpec, password, salt);
			secretKeyCacheEntry = new SecretKeyCacheEntry(secretKey, salt);
			
			secretKeyReadCache.put(cipherSpecWithSalt, secretKeyCacheEntry);
			
			logger.log(Level.INFO, "Created new secret key and added to cache, with salt "+StringUtil.toHex(salt));
			
			if (secretKeyReadCache.size() > secretKeyReadCacheSize) {
				CipherSpecWithSalt firstKey = secretKeyReadCache.keySet().iterator().next();
				secretKeyReadCache.remove(firstKey);
				
				logger.log(Level.INFO, "Removed oldest secret key from cache.");				
			}
			
			return secretKey;
		}
	}		
	
	public static class SaltedSecretKey implements SecretKey {
		private SecretKey secretKey;
		private byte[] salt;
		
		public SaltedSecretKey(SecretKey secretKey, byte[] salt) {
			this.secretKey = secretKey;
			this.salt = salt;
		}

		@Override
		public String getAlgorithm() {
			return secretKey.getAlgorithm();
		}

		@Override
		public String getFormat() {
			return secretKey.getFormat();
		}

		@Override
		public byte[] getEncoded() {
			return secretKey.getEncoded();
		}		
	}
	
	public static class SecretKeyCacheEntry {
		private SecretKey secretKey;
		private byte[] salt;
		private int useCount;

		public SecretKeyCacheEntry(SecretKey secretKey, byte[] salt) {
			this.secretKey = secretKey;
			this.salt = salt;
			this.useCount = 0;
		}

		public SecretKey getSecretKey() {
			return secretKey;
		}

		public byte[] getSalt() {
			return salt;
		}
		
		public int getUseCount() {
			return useCount;
		}
		
		public void incUseCount() {
			useCount++;
		}
	}
	
	private static class CipherSpecWithSalt {
		private CipherSpec cipherSpec;
		private byte[] salt;
		
		public CipherSpecWithSalt(CipherSpec cipherSpec, byte[] salt) {
			this.cipherSpec = cipherSpec;
			this.salt = salt;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((cipherSpec == null) ? 0 : cipherSpec.hashCode());
			result = prime * result + Arrays.hashCode(salt);
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CipherSpecWithSalt other = (CipherSpecWithSalt) obj;
			if (cipherSpec == null) {
				if (other.cipherSpec != null)
					return false;
			} else if (!cipherSpec.equals(other.cipherSpec))
				return false;
			if (!Arrays.equals(salt, other.salt))
				return false;
			return true;
		}			
	}
}
