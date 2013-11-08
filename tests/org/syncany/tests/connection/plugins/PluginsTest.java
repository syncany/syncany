package org.syncany.tests.connection.plugins;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;

public class PluginsTest {
	@Test
	public void testPluginsList() {
		Collection<Plugin> pluginList = Plugins.list();		
		
		List<String> expectedPluginIds = Arrays.asList(new String[] { "local", "ftp", "s3" });
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
		assertNotNull(Plugins.get("ftp"));
	}
}
