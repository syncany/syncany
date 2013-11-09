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
package org.syncany.connection.plugins;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.syncany.util.ClasspathUtil;
import org.syncany.util.StringUtil;

/**
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Plugins {
    public static final Pattern PLUGIN_NAME_REGEX_PLUGIN_INFO = Pattern.compile("org\\.syncany\\.connection\\.plugins\\.([^.]+)\\.[\\w\\d]+Plugin");
    public static final String PLUGIN_FQCN_PREFIX = "org.syncany.connection.plugins.";
    public static final String PLUGIN_FQCN_SUFFIX = "Plugin";
    public static final String PLUGIN_FQCN_PATTERN = PLUGIN_FQCN_PREFIX+"%s.%s"+PLUGIN_FQCN_SUFFIX;
	
	private static final Logger logger = Logger.getLogger(Plugins.class.getSimpleName());
	private static final Map<String, Plugin> plugins = new TreeMap<String, Plugin>();
	private static boolean loaded = false;

	private static void load() {
		if (loaded) {
			return;
		}

		for (String className : ClasspathUtil.getClasspathClasses().values()) {
			if (!className.startsWith(PLUGIN_FQCN_PREFIX) || !className.endsWith(PLUGIN_FQCN_SUFFIX)) {
				continue;
			}

			Matcher m = PLUGIN_NAME_REGEX_PLUGIN_INFO.matcher(className);

			if (m.matches()) {
				loadPlugin(m.group(1), className);
			}
		}
		
		loaded = true;
	}

	public static Collection<Plugin> list() {
		load();
		return plugins.values();
	}

	/**
	 * Loads the plugin by a given ID.
	 * 
	 * <p>
	 * Does not call the list() method to boost performance.
	 * 
	 * @param pluginId
	 * @return
	 */
	public static Plugin get(String pluginId) {
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

	private static void loadPlugin(String pluginId) {
		String className = String.format(PLUGIN_FQCN_PATTERN, pluginId, StringUtil.toCamelCase(pluginId));
		loadPlugin(pluginId, className);
	}

	private static void loadPlugin(String pluginId, String className) {
		// Already loaded
		if (plugins.containsKey(pluginId)) {
			return;
		}

		// Try to load!
		try {
			Class<?> pluginInfoClass = Class.forName(className);
			Plugin pluginInfo = (Plugin) pluginInfoClass.newInstance();

			plugins.put(pluginId, pluginInfo);
		} 
		catch (Exception ex) {
			logger.log(Level.WARNING, "Could not load plugin : " + className);
		}
	}
}
