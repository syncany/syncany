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
package org.syncany.tests.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.syncany.config.Logging;
import org.syncany.crypto.CipherException;
import org.syncany.crypto.CipherParams;
import org.syncany.crypto.CipherUtil;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.util.StringUtil;

public class CipherUtilMasterKeyCreationTest {
	private static final Logger logger = Logger.getLogger(CipherUtilMasterKeyCreationTest.class.getSimpleName());
		
	static {
		Logging.init();
	}
	
	@Test
	public void testCreateMasterKeyWithSalt() throws CipherException {
		long timeStart = System.currentTimeMillis();
		
		SaltedSecretKey masterKeyForPasswordTestAndSalt123 = CipherUtil.createMasterKey("Test", new byte[] { 1, 2, 3 });
		
		long timeEnd = System.currentTimeMillis();
		long timeDuration = timeEnd - timeStart;

		logger.log(Level.INFO, "Creating master key took "+timeDuration+"ms:");
		logger.log(Level.INFO, " - Key:  "+StringUtil.toHex(masterKeyForPasswordTestAndSalt123.getEncoded()));
		logger.log(Level.INFO, " - Salt: "+StringUtil.toHex(masterKeyForPasswordTestAndSalt123.getSalt()));
						
		assertEquals("010203", StringUtil.toHex(masterKeyForPasswordTestAndSalt123.getSalt()));
		assertEquals("44fda24d53b29828b62c362529bd9df5c8a92c2736bcae3a28b3d7b44488e36e246106aa5334813028abb2048eeb5e177df1c702d93cf82aeb7b6d59a8534ff0",
			StringUtil.toHex(masterKeyForPasswordTestAndSalt123.getEncoded()));

		assertEquals(CipherParams.MASTER_KEY_SIZE/8, masterKeyForPasswordTestAndSalt123.getEncoded().length); 
		assertEquals("PBKDF2WithHmacSHA1", masterKeyForPasswordTestAndSalt123.getAlgorithm());
		assertEquals("RAW", masterKeyForPasswordTestAndSalt123.getFormat());
		 
		assertTrue(timeDuration > 5000);
	}
	
	@Test
	public void testCreateMasterKeyNoSalt() throws CipherException {
		SaltedSecretKey masterKeyForPasswordTestNoSalt1 = CipherUtil.createMasterKey("Test");
		SaltedSecretKey masterKeyForPasswordTestNoSalt2 = CipherUtil.createMasterKey("Test");
						
		logger.log(Level.INFO, "Key comparison for password 'Test':");
		logger.log(Level.INFO, "- Master key 1: "+StringUtil.toHex(masterKeyForPasswordTestNoSalt1.getEncoded()));
		logger.log(Level.INFO, "     with salt: "+StringUtil.toHex(masterKeyForPasswordTestNoSalt1.getSalt()));
		logger.log(Level.INFO, "- Master key 2: "+StringUtil.toHex(masterKeyForPasswordTestNoSalt2.getEncoded()));
		logger.log(Level.INFO, "     with salt: "+StringUtil.toHex(masterKeyForPasswordTestNoSalt2.getSalt()));
		
		assertFalse(Arrays.equals(masterKeyForPasswordTestNoSalt1.getSalt(), masterKeyForPasswordTestNoSalt2.getSalt()));
		assertFalse(Arrays.equals(masterKeyForPasswordTestNoSalt1.getEncoded(), masterKeyForPasswordTestNoSalt2.getEncoded()));
	}	
}
