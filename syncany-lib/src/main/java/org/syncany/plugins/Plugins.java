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
package org.syncany.plugins;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.reflections.Reflections;
import org.syncany.util.StringUtil;

/**
 * This class loads and manages all the {@link Plugin}s loaded in the classpath.
 * It provides two public methods: 
 * 
 * <ul>
 *  <li>{@link #list()} returns a list of all loaded plugins (as per classpath)</li>
 *  <li>{@link #get(String) get()} returns a specific plugin, defined by a name</li>
 * </ul>   
 *  
 * @see Plugin
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Plugins {
	private static final Logger logger = Logger.getLogger(Plugins.class.getSimpleName());
	private static final Reflections reflections = new Reflections(Plugin.class.getPackage().getName());
	private static final Map<String, Plugin> plugins = new TreeMap<String, Plugin>();

	
	/**
	 * Loads and returns a list of all available
	 * {@link Plugin}s. 
	 */
	public static List<Plugin> list() {
		loadPlugins();
		return new ArrayList<Plugin>(plugins.values());
	}
	
	/**
	 * Loads and returns a list of all {@link Plugin}s
	 * matching the given subclass.
	 */
	public static <T extends Plugin> List<T> list(Class<T> pluginClass) {
		loadPlugins();
		List<T> matchingPlugins = new ArrayList<T>();
		
		for (Plugin plugin : plugins.values()) {
			if (pluginClass.isInstance(plugin)) {
				matchingPlugins.add(pluginClass.cast(plugin));
			}
		}
		
		return matchingPlugins;
	}

	/**
	 * Loads the {@link Plugin} by a given identifier.
	 * 
	 * <p>Note: Unlike the {@link #list()} method, this method is not expected 
	 * to take long, because there is no need to read all JARs in the classpath.
	 * 
	 * @param pluginId Identifier of the plugin, as defined by {@link Plugin#getId() the plugin ID)
	 * @return Returns an instance of a plugin, or <tt>null</tt> if no plugin with the given identifier can be found
	 */
	public static Plugin get(String pluginId) {
		if (pluginId == null) {
			return null;
		}
		
		loadPlugin(pluginId);
		
		if (plugins.containsKey(pluginId)) {
			return plugins.get(pluginId);
		}
		else {
			return null;
		}
	}
	
	public static <T extends Plugin> T get(String pluginId, Class<T> pluginClass) {
		Plugin plugin = get(pluginId);
		
		if (pluginId == null || !pluginClass.isInstance(plugin)) {
			return null;
		}
		else {
			return pluginClass.cast(plugin);
		}
	}
	
	private static void loadPlugin(String pluginId) {
		if (plugins.containsKey(pluginId)) {
			return;
		}
		
		// TODO [low] Duplicate code with loadPlugins()
		
		for (Class<? extends Plugin> pluginClass : reflections.getSubTypesOf(Plugin.class)) {
			boolean canInstantiate = !Modifier.isAbstract(pluginClass.getModifiers());
			String candidatePluginId = pluginClass.getSimpleName().replace(Plugin.class.getSimpleName(), "").toLowerCase();
			
			if (canInstantiate && candidatePluginId.equals(pluginId)) {
				try {
					Plugin plugin = (Plugin) pluginClass.newInstance();			
					plugins.put(plugin.getId(), plugin);
				}
				catch (Exception e) {
					logger.log(Level.WARNING, "Could not load plugin (2): " + pluginClass.getName(), e);
				}
			}
		}
	}

	private static void loadPlugins() {
		for (Class<? extends Plugin> pluginClass : reflections.getSubTypesOf(Plugin.class)) {
			boolean canInstantiate = !Modifier.isAbstract(pluginClass.getModifiers());
			String pluginId = pluginClass.getSimpleName().replace(Plugin.class.getSimpleName(), "").toLowerCase();
			
			if (canInstantiate && !plugins.containsKey(pluginId)) {
				try {
					Plugin plugin = (Plugin) pluginClass.newInstance();			
					plugins.put(plugin.getId(), plugin);
				}
				catch (Exception e) {
					logger.log(Level.WARNING, "Could not load plugin (2): " + pluginClass.getName(), e);
				}
			}
		}
	}
}
