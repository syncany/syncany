package org.syncany.crypto;

import java.util.Map;
import java.util.TreeMap;

/**
 * Defines the supported Cipher suites.
 * 
 * In general:
 * - IV size = block size
 *
 */
public class CipherSpecs {
	private static final Map<Integer, CipherSpec> cipherSpecs = new TreeMap<Integer, CipherSpec>();
	
	static {
		CipherSpec[] tmpCipherSpecs = new CipherSpec[] {
			// Standard
			new CipherSpec(0x01, "AES/GCM/NoPadding", 128, false, true, 128, true),
			new CipherSpec(0x02, "Twofish/GCM/NoPadding", 128, false, true, 128, true),
			
			// Unlimited crypto
			new CipherSpec(0x03, "AES/GCM/NoPadding", 256, true, true, 128, true),
			new CipherSpec(0x04, "Twofish/GCM/NoPadding", 256, true, true, 128, true)
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
