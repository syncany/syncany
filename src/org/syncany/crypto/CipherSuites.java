package org.syncany.crypto;

import java.util.Map;
import java.util.TreeMap;

public class CipherSuites {
	private static final Map<Integer, CipherSuite> cipherSuites = new TreeMap<Integer, CipherSuite>();
	
	static {
		CipherSuite[] tmpCipherSuites = new CipherSuite[] {
			// Standard
			new CipherSuite(0x01, "AES/GCM/NoPadding", 128, false, true, 128),
			new CipherSuite(0x02, "Twofish/GCM/NoPadding", 128, false, true, 128),
			
			// Unlimited crypto
			new CipherSuite(0x03, "AES/GCM/NoPadding", 256, true, true, 256),
			new CipherSuite(0x04, "Twofish/GCM/NoPadding", 256, true, true, 256)
		};		
		
		for (CipherSuite cryptoSuite : tmpCipherSuites) {
			cipherSuites.put(cryptoSuite.getId(), cryptoSuite);
		}
	}
	
	public static Map<Integer, CipherSuite> getAvailableCipherSuites() {
		return cipherSuites;
	}
	
	public static CipherSuite getCipherSuite(int id) {
		return cipherSuites.get(id);
	}
}
