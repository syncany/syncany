package org.syncany.tests.crypto;

import static org.junit.Assert.assertEquals;
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
		assertEquals(availableCipherSpecs.get(1).getAlgorithm(), "AES/GCM/NoPadding");		
	}
	
	@Test
	public void testCipherSpec2() {
		CipherSpec twofish128CipherSpec = CipherSpecs.getCipherSpec(2);
		
		assertEquals(twofish128CipherSpec.getId(), 2);
		assertEquals(twofish128CipherSpec.getAlgorithm(), "Twofish/GCM/NoPadding");
		assertEquals(twofish128CipherSpec.getKeySize(), 128);
		assertEquals(twofish128CipherSpec.getIvSize(), 128);
	}
	
	@Test(expected=Exception.class)
	public void testNewEcbCipherSpec() {
		new CipherSpec(0xFF, "AES/ECB/PKCS5Padding", 128, 128, false);
	}
	
	@Test
	public void testCipherSpecHashCodeEquals() {
		CipherSpec cipherSpec1 = CipherSpecs.getCipherSpec(1);
		CipherSpec cipherSpec2 = CipherSpecs.getCipherSpec(2);
		
		assertNotSame(cipherSpec1.hashCode(), cipherSpec2.hashCode());
		assertNotSame(cipherSpec1, cipherSpec2);
		assertEquals(cipherSpec1, new CipherSpec(0x01, "AES/GCM/NoPadding", 128, 128, false));
	}
}
