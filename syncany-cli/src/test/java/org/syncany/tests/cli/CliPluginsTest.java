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
package org.syncany.tests.cli;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;

public class CliPluginsTest {
	@Test
	public void testPluginsList() {
		Collection<Plugin> pluginList = Plugins.list();		
		
		List<String> expectedPluginIds = Arrays.asList(new String[] { "local", "unreliable_local" });
		List<String> actualPluginIds = new ArrayList<String>();
		
		for (Plugin plugin : pluginList) {
			actualPluginIds.add(plugin.getId());
		}
		
		assertTrue(expectedPluginIds.containsAll(actualPluginIds));
		assertTrue(actualPluginIds.containsAll(expectedPluginIds));
	}
	
	@Test
	public void testNonExistingPlugin() {
		assertNull(Plugins.get("non-existing"));
	}
	
	@Test
	public void testExistingPlugin() {
		assertNotNull(Plugins.get("local"));
		assertNotNull(Plugins.get("unreliable_local"));
	}
}
