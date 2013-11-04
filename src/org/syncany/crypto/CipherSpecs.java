package org.syncany.crypto;

import java.util.Map;
import java.util.TreeMap;

/**
 * Defines the supported Cipher suites.
 * 
 * IMPORTANT: 
 *   The block cipher mode MUST be authenticated. Unauthenticated
 *   modes are not supported and will be rejected in the MultiCipherOutputStream.
 */
public class CipherSpecs {
	private static final Map<Integer, CipherSpec> cipherSpecs = new TreeMap<Integer, CipherSpec>();
	
	static {
		CipherSpec[] tmpCipherSpecs = new CipherSpec[] {
			// Standard
			new CipherSpec(0x01, "AES/GCM/NoPadding", 128, 128, false),
			new CipherSpec(0x02, "Twofish/GCM/NoPadding", 128, 128, false),
			
			// Unlimited crypto
			new CipherSpec(0x03, "AES/GCM/NoPadding", 256, 128, true),
			new CipherSpec(0x04, "Twofish/GCM/NoPadding", 256, 128, true)
		};		
		
		for (CipherSpec cipherSpec : tmpCipherSpecs) {
			cipherSpecs.put(cipherSpec.getId(), cipherSpec);
		}
	}
	
	public static Map<Integer, CipherSpec> getAvailableCipherSpecs() {
		return cipherSpecs;
	}
	
	public static CipherSpec getCipherSpec(int id) {
		return cipherSpecs.get(id);
	}
}
