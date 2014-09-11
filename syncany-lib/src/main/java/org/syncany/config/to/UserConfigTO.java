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
package org.syncany.config.to;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.ConfigException;

/**
 * The user config transfer object is a helper data structure that allows storing
 * a user's global system settings such as system properties.
 * 
 * <p>It uses the Simple framework for XML serialization, and its corresponding
 * annotation-based configuration.  
 *  
 * @see <a href="http://simple.sourceforge.net/">Simple framework</a> at simple.sourceforge.net
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
@Root(name = "userConfig", strict = false)
public class UserConfigTO {
	@ElementMap(name = "systemProperties", entry = "property", key = "name", required = false, attribute = true)
	private TreeMap<String, String> systemProperties;

	@Element(name = "preventStandby", required = false)
	private boolean preventStandby;

	public UserConfigTO() {
		this.systemProperties = new TreeMap<String, String>();
		this.preventStandby = false;
	}

	public Map<String, String> getSystemProperties() {
		return systemProperties;
	}

	public boolean preventStandbyEnabled() {
		return preventStandby;
	}

	public static UserConfigTO load(File file) throws ConfigException {
		try {
			return new Persister().read(UserConfigTO.class, file);
		}
		catch (Exception e) {
			throw new ConfigException("User config file cannot be read or is invalid: " + file, e);
		}
	}

	public static void save(UserConfigTO userConfigTO, File file) throws ConfigException {
		try {
			new Persister().write(userConfigTO, file);
		}
		catch (Exception e) {
			throw new ConfigException("Cannot write user config to file " + file, e);
		}
	}
}
