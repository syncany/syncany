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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.syncany.crypto.specs.AesGcm128CipherSpec;
import org.syncany.crypto.specs.AesGcm256CipherSpec;
import org.syncany.crypto.specs.TwofishGcm128CipherSpec;
import org.syncany.crypto.specs.TwofishGcm256CipherSpec;

/**
 * Defines and identifies the application supported {@link CipherSpec}s.
 * 
 * <p>These cipher specs are used by the {@link MultiCipherOutputStream} to encrypt
 * data, and by the {@link MultiCipherInputStream} to decrypt data. The cipher spec
 * identifiers are used in the crypto format header to identify the crypto algorithms
 * used for encryption.
 * 
 * <p>The class defines a well defined (and developer-approved) set of allowed
 * cipher algorithms, modes and key sizes. The number of allowed ciphers is greatly
 * restricted to follow the application-specific security standards. Most prominently,
 * this includes:
 * 
 * <ul>
 *   <li>The block cipher mode must be authenticated (GCM, EAX, etc.). Unauthenticated
 *       modes are not supported and will be rejected by the {@link CipherSpec} sanity checks.
 *   <li>The block cipher mode must require an initialization vector (IV). Modes that do 
 *       not require an IV (e.g. ECB) will be rejected by the {@link CipherSpec} sanity checks.
 * </ul>
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class CipherSpecs {
	private static final Map<Integer, CipherSpec> cipherSpecs = new TreeMap<Integer, CipherSpec>();

	/*
	 * WARNING: The cipher spec identifiers are written to the MultiCipherOutputStream and read by the MultiCipherInputStream. The identifiers MUST
	 * NOT be changed, because this will make decryption of already encrypted data impossible!
	 */
	public static final int AES_128_GCM = 0x01;
	public static final int TWOFISH_128_GCM = 0x02;
	public static final int AES_256_GCM = 0x03;
	public static final int TWOFISH_256_GCM = 0x04;

	public static final int[] DEFAULT_CIPHER_SPECS = new int[] { CipherSpecs.AES_128_GCM, CipherSpecs.TWOFISH_128_GCM };

	static {
		CipherSpec[] tmpCipherSpecs = new CipherSpec[] {
				// Standard
				new AesGcm128CipherSpec(),
				new TwofishGcm128CipherSpec(),

				// Unlimited crypto
				new AesGcm256CipherSpec(),
				new TwofishGcm256CipherSpec() 
			};

		for (CipherSpec cipherSpec : tmpCipherSpecs) {
			registerCipherSpec(cipherSpec.getId(), cipherSpec);
		}
	}

	/**
	 * Returns a list of available/registered {@link CipherSpec}s. Refer to the 
	 * {@link CipherSpecs class description} for a more detailed explanation.
	 */
	public static Map<Integer, CipherSpec> getAvailableCipherSpecs() {
		return cipherSpecs;
	}
	
	/**
	 * Returns the default {@link CipherSpec}s used by the application.
	 */
	public static List<CipherSpec> getDefaultCipherSpecs() {
		List<CipherSpec> cipherSpecs = new ArrayList<CipherSpec>();
		
		for (int cipherSpecId : DEFAULT_CIPHER_SPECS) { 
			cipherSpecs.add(getCipherSpec(cipherSpecId));
		}	
		
		return cipherSpecs;
	}

	/**
	 * Retrieves an available/registered {@link CipherSpec} using the cipher spec identifier
	 * defined in this class.
	 * 
	 * @param id Identifier of the cipher spec
	 * @return A cipher spec, or <tt>null</tt> if no cipher spec with this identifier is registered
	 */
	public static CipherSpec getCipherSpec(int id) {
		return cipherSpecs.get(id);
	}

	/**
	 * Register a new cipher spec.
	 * 
	 * <p>Note: Registering a cipher spec locally does not make it available on all clients. Unless
	 * a cipher spec is registered before a client tries to decrypt data using the {@link MultiCipherInputStream},
	 * the decryption process will fail. 
	 * 
	 * @param id Identifier of the cipher spec
	 */
	public static void registerCipherSpec(int id, CipherSpec cipherSpec) {
		cipherSpecs.put(id, cipherSpec);
	}
}
