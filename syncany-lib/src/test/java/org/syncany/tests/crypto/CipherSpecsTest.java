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
package org.syncany.tests.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import java.util.Map;

import org.junit.Test;
import org.syncany.crypto.CipherSpec;
import org.syncany.crypto.CipherSpecs;

public class CipherSpecsTest {
	@Test
	public void testCipherSpecs() {
		Map<Integer, CipherSpec> availableCipherSpecs = CipherSpecs.getAvailableCipherSpecs();
		
		assertEquals(4, availableCipherSpecs.size());
		assertEquals(availableCipherSpecs.get(CipherSpecs.AES_128_GCM).getAlgorithm(), "AES/GCM/NoPadding");		
	}
	
	@Test
	public void testCipherSpec2() {
		CipherSpec twofish128CipherSpec = CipherSpecs.getCipherSpec(CipherSpecs.TWOFISH_128_GCM);
		
		assertEquals(twofish128CipherSpec.getId(), 2);
		assertEquals(twofish128CipherSpec.getAlgorithm(), "Twofish/GCM/NoPadding");
		assertEquals(twofish128CipherSpec.getKeySize(), 128);
		assertEquals(twofish128CipherSpec.getIvSize(), 128);
		assertEquals(twofish128CipherSpec.needsUnlimitedStrength(), false);
		assertNotNull(twofish128CipherSpec.toString());
	}
	
	@Test
	public void testCipherSpecHashCodeEquals() {
		CipherSpec cipherSpec1 = CipherSpecs.getCipherSpec(CipherSpecs.AES_128_GCM);
		CipherSpec cipherSpec2 = CipherSpecs.getCipherSpec(CipherSpecs.TWOFISH_128_GCM);
		
		assertNotSame(cipherSpec1.hashCode(), cipherSpec2.hashCode());
		assertNotSame(cipherSpec1, cipherSpec2);
		assertEquals(0x01, cipherSpec1.getId());
	}
}
