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
package org.syncany.connection.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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
	public static final String PLUGIN_FQCN_PREFIX = Plugin.class.getPackage().getName();
	public static final String PLUGIN_FQCN_SUFFIX = Plugin.class.getSimpleName();
	public static final String PLUGIN_FQCN_FORMAT = PLUGIN_FQCN_PREFIX + ".%s.%s" + PLUGIN_FQCN_SUFFIX;
	public static final Pattern PLUGIN_FQCN_REGEX = Pattern.compile(PLUGIN_FQCN_PREFIX + "\\.([^.]+)\\.[\\w\\d]+" + PLUGIN_FQCN_SUFFIX);

	private static final Logger logger = Logger.getLogger(Plugins.class.getSimpleName());
	private static final Reflections reflections = new Reflections(PLUGIN_FQCN_PREFIX);
	private static final Map<String, Plugin> plugins = new TreeMap<String, Plugin>();
	private static boolean loaded = false;

	/**
	 * Loads and returns a list of all available {@link Plugin}s.
	 * 
	 * <p>Note: This method might take a few milliseconds longer when it is
	 * first called, because it loads and opens all JARs in the classpath
	 * to look for matching name patterns.
	 * 
	 * @return Returns a collection of all loaded plugins 
	 */
	public static List<Plugin> list() {
		load();
		return new ArrayList<Plugin>(plugins.values());
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
		
		// If already loaded, get from list
		if (plugins.containsKey(pluginId)) {
			return plugins.get(pluginId);
		}

		// Try to load via name
		loadPlugin(pluginId);

		if (plugins.containsKey(pluginId)) {
			return plugins.get(pluginId);
		}

		// Not found!
		return null;
	}

	private static void load() {
		if (loaded) {
			return;
		}

		for (Class<? extends Plugin> pluginClass : reflections.getSubTypesOf(Plugin.class)) {
			loadPlugin(pluginClass);			
		}
		
		loaded = true;
	}

	private static void loadPlugin(String pluginId) {
		String className = String.format(PLUGIN_FQCN_FORMAT, pluginId, StringUtil.toCamelCase(pluginId));
		loadPlugin(pluginId, className);
	}

	private static void loadPlugin(String pluginId, String className) {
		// Already loaded
		if (plugins.containsKey(pluginId)) {
			return;
		}

		// Try to load!
		try {
			loadPlugin(Class.forName(className));		
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Could not load plugin (1): " + className, e);
		}
	}
	
	private static void loadPlugin(Class<?> pluginClass) {		
		try {
			Plugin plugin = (Plugin) pluginClass.newInstance();			
			plugins.put(plugin.getId(), plugin);
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Could not load plugin (2): " + pluginClass.getName(), e);
		}
	}
}
