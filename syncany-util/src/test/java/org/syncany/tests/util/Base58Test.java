/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.util.Arrays;

import org.junit.Test;
import org.syncany.util.Base58;

public class Base58Test {
	
	@Test
    public void testEncode() throws Exception {
        byte[] testbytes = "Hello World".getBytes();
        assertEquals("JxF12TrwUP45BMd", Base58.encode(testbytes));
        
        BigInteger bi = BigInteger.valueOf(3471844090L);
        assertEquals("16Ho7Hs", Base58.encode(bi.toByteArray()));
        
        byte[] zeroBytes1 = new byte[1];
        assertEquals("1", Base58.encode(zeroBytes1));
        
        byte[] zeroBytes7 = new byte[7];
        assertEquals("1111111", Base58.encode(zeroBytes7));

        // test empty encode
        assertEquals("", Base58.encode(new byte[0]));
    }

    @Test
    public void testDecode() throws Exception {
        byte[] testbytes = "Hello World".getBytes();
        byte[] actualbytes = Base58.decode("JxF12TrwUP45BMd");
        assertTrue(new String(actualbytes), Arrays.equals(testbytes, actualbytes));
        
        assertTrue("1", Arrays.equals(Base58.decode("1"), new byte[1]));
        assertTrue("1111", Arrays.equals(Base58.decode("1111"), new byte[4]));
        
        try {
            Base58.decode("This isn't valid base58");
            fail();
        } catch (RuntimeException e) {
            // expected
        }

        // Test decode of empty String.
        assertEquals(0, Base58.decode("").length);
    }
}
