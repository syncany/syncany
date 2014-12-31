/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com>
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

/**
 * The cipher session is used by the {@link MultiCipherOutputStream} and the
 * {@link MultiCipherInputStream} to reference the application's master key,
 * and to temporarily store and retrieve derived secret keys.
 *
 * <p>While a cipher session does not create a master key, it creates and manages
 * the derived keys using the {@link CipherUtil} class:
 *
 * <ul>
 *   <li>Keys used by {@link MultiCipherOutputStream} (for writing new files) are
 *       reused a number of times before a new salted key is created. The main
 *       purpose of reusing keys is to increase performance. Because the master
 *       key is cryptographically strong, the derived keys can be reused a few
 *       times without any drawbacks on security. The class keeps one secret key
 *       per {@link CipherSpec}.
 *
 *   <li>Keys used by {@link MultiCipherInputStream} (when reading files) are
 *       cached in order to minimize the amount of keys that have to be created when
 *       files are processed.
 * </ul>
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class CipherSession {
	private static final Logger logger = Logger.getLogger(CipherSession.class.getSimpleName());
	private static final int DEFAULT_SECRET_KEY_READ_CACHE_SIZE = 20;
	private static final int DEFAULT_SECRET_KEY_WRITE_REUSE_COUNT = 100;

	private SecretKey masterKey;

	private Map<CipherSpecWithSalt, SecretKeyCacheEntry> secretKeyReadCache;
	private int secretKeyReadCacheSize;

	private Map<CipherSpec, SecretKeyCacheEntry> secretKeyWriteCache;
	private int secretKeyWriteReuseCount;

	/**
	 * Creates a new cipher session, using the given master key. Derived keys will be created
	 * from that master key.
	 *
	 * <p>The default settings for read/write key cache will be used. Refer to
	 * {@link CipherSession the class description} for more details. Default values:
	 * {@link #DEFAULT_SECRET_KEY_READ_CACHE_SIZE} and {@link #DEFAULT_SECRET_KEY_WRITE_REUSE_COUNT}
	 *
	 * @param masterKey The master key, used for deriving new read/write keys
	 */
	public CipherSession(SaltedSecretKey masterKey) {
		this(masterKey, DEFAULT_SECRET_KEY_READ_CACHE_SIZE, DEFAULT_SECRET_KEY_WRITE_REUSE_COUNT);
	}

	/**
	 * Creates a new cipher session, using the given master key. Derived keys will be created
	 * from that master key.
	 *
	 * <p>This method expects a reuse-count for write keys and a cache size for the read-key cache.
	 * Refer to {@link CipherSession the class description} for more details.
	 *
	 * @param masterKey The master key, used for deriving new read/write
	 * @param secretKeyReadCacheSize Number of read keys to store in the cache (higher means more performance, but more memory usage)
	 * @param secretKeyWriteReuseCount Number of times to reuse a write key (higher means more performance, but lower security)
	 */
	public CipherSession(SaltedSecretKey masterKey, int secretKeyReadCacheSize, int secretKeyWriteReuseCount) {
		this.masterKey = masterKey;

		this.secretKeyReadCache = new LinkedHashMap<CipherSpecWithSalt, SecretKeyCacheEntry>();
		this.secretKeyReadCacheSize = secretKeyReadCacheSize;

		this.secretKeyWriteCache = new HashMap<CipherSpec, SecretKeyCacheEntry>();
		this.secretKeyWriteReuseCount = secretKeyWriteReuseCount;
	}

	/**
	 * Returns the master key
	 */
	public SecretKey getMasterKey() {
		return masterKey;
	}

	/**
	 * Creates or retrieves a derived write secret key and updates the cache and re-use count. If a key is reused more
	 * than the threshold defined in {@link #secretKeyWriteReuseCount} (as set in {@link #CipherSession(SaltedSecretKey, int, int) the constructor},
	 * a new key is created and added to the cache.
	 *
	 * <p>If a new key needs to be created, {@link CipherUtil} is used to do so.
	 *
	 * <p>Contrary to the read cache, the write cache key is a only {@link CipherSpec}, i.e. only one secret key
	 * per cipher spec can be held in the cache.
	 *
	 * @param cipherSpec Defines the type of key to be created (or retrieved); used as key for the cache retrieval
	 * @return Returns a newly created secret key or a cached key
	 * @throws Exception If an error occurs with key creation
	 */
	public SaltedSecretKey getWriteSecretKey(CipherSpec cipherSpec) throws Exception {
		SecretKeyCacheEntry secretKeyCacheEntry = secretKeyWriteCache.get(cipherSpec);

		// Remove key if use more than X times
		if (secretKeyCacheEntry != null && secretKeyCacheEntry.getUseCount() >= secretKeyWriteReuseCount) {
			logger.log(Level.FINE, "- Removed WRITE secret key from cache, because it was used " + secretKeyCacheEntry.getUseCount() + " times.");

			secretKeyWriteCache.remove(cipherSpec);
			secretKeyCacheEntry = null;
		}

		// Return cached key, or create a new one
		if (secretKeyCacheEntry != null) {
			secretKeyCacheEntry.increaseUseCount();

			logger.log(Level.FINE, "- Using CACHED WRITE secret key " + secretKeyCacheEntry.getSaltedSecretKey().getAlgorithm() + ", with salt "
					+ StringUtil.toHex(secretKeyCacheEntry.getSaltedSecretKey().getSalt()));
			return secretKeyCacheEntry.getSaltedSecretKey();
		}
		else {
			SaltedSecretKey saltedSecretKey = createSaltedSecretKey(cipherSpec);

			secretKeyCacheEntry = new SecretKeyCacheEntry(saltedSecretKey);
			secretKeyWriteCache.put(cipherSpec, secretKeyCacheEntry);

			logger.log(Level.FINE, "- Created NEW WRITE secret key " + secretKeyCacheEntry.getSaltedSecretKey().getAlgorithm()
					+ ", and added to cache, with salt " + StringUtil.toHex(saltedSecretKey.getSalt()));
			return saltedSecretKey;
		}
	}

	/**
	 * Creates a new secret key or retrieves it from the read cache. If the given cipher spec / salt combination
	 * is found in the cache, the cached secret key is returned. If not, a new key is created. Keys are removed
	 * from the cache when the cache reached the size defined by {@link #secretKeyReadCacheSize} (as set in
	 * {@link #CipherSession(SaltedSecretKey, int, int) the constructor}.
	 *
	 * <p>If a new key needs to be created, {@link CipherUtil} is used to do so.
	 *
	 * <p>Contrary to the write cache, the read cache key is a combination of {@link CipherSpec} and a salt. For
	 * each cipher spec, multiple salted keys can reside in the cache at the same time.
	 *
	 * @param cipherSpec Defines the type of key to be created (or retrieved); used as one part of the key for cache retrieval
	 * @param salt Defines the salt for the key to be created (or retrieved); used as one part of the key for cache retrieval
	 * @return Returns a newly created secret key or a cached key
	 * @throws Exception If an error occurs with key creation
	 */
	public SaltedSecretKey getReadSecretKey(CipherSpec cipherSpec, byte[] salt) throws Exception {
		CipherSpecWithSalt cipherSpecWithSalt = new CipherSpecWithSalt(cipherSpec, salt);
		SecretKeyCacheEntry secretKeyCacheEntry = secretKeyReadCache.get(cipherSpecWithSalt);

		if (secretKeyCacheEntry != null) {
			logger.log(Level.FINE, "- Using CACHED READ secret key " + secretKeyCacheEntry.getSaltedSecretKey().getAlgorithm() + ", with salt "
					+ StringUtil.toHex(salt));
			return secretKeyCacheEntry.getSaltedSecretKey();
		}
		else {
			if (secretKeyReadCache.size() >= secretKeyReadCacheSize) {
				CipherSpecWithSalt firstKey = secretKeyReadCache.keySet().iterator().next();
				secretKeyReadCache.remove(firstKey);

				logger.log(Level.FINE, "- Removed oldest READ secret key from cache.");
			}

			SaltedSecretKey saltedSecretKey = createSaltedSecretKey(cipherSpec, salt);
			secretKeyCacheEntry = new SecretKeyCacheEntry(saltedSecretKey);

			secretKeyReadCache.put(cipherSpecWithSalt, secretKeyCacheEntry);

			logger.log(Level.FINE, "- Created NEW READ secret key " + secretKeyCacheEntry.getSaltedSecretKey().getAlgorithm()
					+ ", and added to cache, with salt " + StringUtil.toHex(salt));
			return saltedSecretKey;
		}
	}

	private SaltedSecretKey createSaltedSecretKey(CipherSpec cipherSpec) throws InvalidKeySpecException, NoSuchAlgorithmException,
	NoSuchProviderException {
		byte[] salt = CipherUtil.createRandomArray(MultiCipherOutputStream.SALT_SIZE);
		return createSaltedSecretKey(cipherSpec, salt);
	}

	private SaltedSecretKey createSaltedSecretKey(CipherSpec cipherSpec, byte[] salt) throws InvalidKeySpecException, NoSuchAlgorithmException,
	NoSuchProviderException {
		return CipherUtil.createDerivedKey(masterKey, salt, cipherSpec);
	}

	private static class SecretKeyCacheEntry {
		private SaltedSecretKey saltedSecretKey;
		private int useCount;

		public SecretKeyCacheEntry(SaltedSecretKey saltedSecretKey) {
			this.saltedSecretKey = saltedSecretKey;
			this.useCount = 1;
		}

		public SaltedSecretKey getSaltedSecretKey() {
			return saltedSecretKey;
		}

		public int getUseCount() {
			return useCount;
		}

		public void increaseUseCount() {
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
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof CipherSpecWithSalt)) {
				return false;
			}
			CipherSpecWithSalt other = (CipherSpecWithSalt) obj;
			if (cipherSpec == null) {
				if (other.cipherSpec != null) {
					return false;
				}
			}
			else if (!cipherSpec.equals(other.cipherSpec)) {
				return false;
			}
			if (!Arrays.equals(salt, other.salt)) {
				return false;
			}
			return true;
		}
	}

}
