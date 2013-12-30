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
package org.syncany.tests.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.syncany.util.JsonHelper;

/**
 * @author vincent
 *
 */
public class JsonHelperTest {
	
	@Test
	public void testSimpleMapToString() {
		Map<String, String> map = new HashMap<>();
		map.put("key_A", "value_A");
		map.put("key_B", "value_B");
		map.put("key_C", "value_C");
		
		String  json = JsonHelper.fromMapToString(map);
		
		Map<String, Object> res = JsonHelper.fromStringToMap(json);
		
		assertTrue(res.containsKey("key_A"));
		assertTrue(res.containsKey("key_B"));
		assertTrue(res.containsKey("key_C"));
		
		assertEquals(res.get("key_A"), "value_A");
		assertEquals(res.get("key_B"), "value_B");
		assertEquals(res.get("key_C"), "value_C");
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testComplexMapToString() {
		Map<String, String> map1 = new HashMap<>();
		map1.put("key_A1", "value_A1");
		map1.put("key_B1", "value_B1");
		map1.put("key_C1", "value_C1");
		
		Map<String, String> map2 = new HashMap<>();
		map2.put("key_A2", "value_A2");
		map2.put("key_B2", "value_B2");
		map2.put("key_C2", "value_C2");
		
		Map<String, String> map3 = new HashMap<>();
		map3.put("key_A3", "value_A3");
		map3.put("key_B3", "value_B3");
		map3.put("key_C3", "value_C3");
		
		Map<String, Object> map = new HashMap<>();
		map.put("key_A", map1);
		map.put("key_B", map2);
		map.put("key_C", map3);
		
		String  json = JsonHelper.fromMapToString(map);
		Map<String, Object> res = JsonHelper.fromStringToMap(json);
		
		
		assertTrue(res.containsKey("key_A"));
		assertTrue(res.containsKey("key_B"));
		assertTrue(res.containsKey("key_C"));
		
		map1 = (Map<String, String>)res.get("key_A");
		map2 = (Map<String, String>)res.get("key_B");
		map3 = (Map<String, String>)res.get("key_C");

		assertEquals(map1.get("key_A1"), "value_A1");
		assertEquals(map1.get("key_B1"), "value_B1");
		assertEquals(map1.get("key_C1"), "value_C1");
		
		assertEquals(map2.get("key_A2"), "value_A2");
		assertEquals(map2.get("key_B2"), "value_B2");
		assertEquals(map2.get("key_C2"), "value_C2");
		
		assertEquals(map3.get("key_A3"), "value_A3");
		assertEquals(map3.get("key_B3"), "value_B3");
		assertEquals(map3.get("key_C3"), "value_C3");
		
		assertEquals(map, res);
	}
}


