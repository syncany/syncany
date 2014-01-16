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

import static org.junit.Assert.*;

import org.junit.Test;
import org.syncany.util.StringUtil;

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
	
	// TODO [low] Test rest of StringUtil
}
