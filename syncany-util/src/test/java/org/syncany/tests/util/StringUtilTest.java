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
package org.syncany.tests.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.syncany.util.StringUtil;
import org.syncany.util.StringUtil.StringJoinListener;

public class StringUtilTest {
	@Test 
	public void testFromHexToHex() {
		assertEquals("abcdeffaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", StringUtil.toHex(StringUtil.fromHex("abcdeffaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")));
		assertEquals("", StringUtil.toHex(StringUtil.fromHex("")));		
	}
	
	@Test(expected=Exception.class)
	public void testFromHexInvalid1() {
		StringUtil.toHex(StringUtil.fromHex("a"));
	}
	
	@Test(expected=Exception.class)
	public void testFromHexInvalid2() {
		StringUtil.toHex(StringUtil.fromHex("INVALID!"));
	}
	
	@Test(expected=Exception.class)
	public void testFromHexInvalid3() {
		StringUtil.toHex(StringUtil.fromHex("INVALID"));
	}
	
	@Test
	public void testStringJoin() {
		assertEquals("a;b;c", StringUtil.join(new String[] { "a",  "b", "c" }, ";"));
		assertEquals("a b c", StringUtil.join(Arrays.asList(new String[] { "a",  "b", "c" }), " "));
		assertEquals("1.9 + 2.8 + 3.7", StringUtil.join(new Double[] { 1.911, 2.833, 3.744 }, " + ", new StringJoinListener<Double>() {
			@Override
			public String getString(Double number) {
				return String.format("%.1f", number);
			}			
		}));
	}
	
	@Test
	public void testToBytesUTF8() {
		assertArrayEquals(new byte[] { 0x00 }, StringUtil.toBytesUTF8("\0"));
		assertArrayEquals(new byte[] { (byte) 0xc3, (byte) 0xa4, (byte) 0xc3, (byte) 0xb6, (byte) 0xc3, (byte) 0xbc }, StringUtil.toBytesUTF8("äöü"));
		assertArrayEquals(new byte[] { (byte) 0xe7, (byte) 0xac, (byte) 0xaa, (byte) 0xe9, (byte) 0xaa, (byte) 0x8f }, StringUtil.toBytesUTF8("笪骏")); 
	}
	
	@Test
	public void testToCamelCase() {
		assertEquals("HelloWorld", StringUtil.toCamelCase("hello world"));
		assertEquals("HelloWorld", StringUtil.toCamelCase("hello_world"));
		assertEquals("HelloWorld", StringUtil.toCamelCase("hello-world"));
		assertEquals("HelloWorld", StringUtil.toCamelCase("hello-World"));
	}
}
