/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
