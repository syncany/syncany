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

import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config.ConfigException;

@Root(name="globalConfig")
@Namespace(reference="http://syncany.org/globalconfig/1")
public class GlobalConfigTO {
	@ElementMap(name="systemProperties", entry="property", key="name", required=false, attribute=true)
	protected Map<String, String> systemProperties;
	
	public static GlobalConfigTO load(File file) throws ConfigException {
		try {
			return new Persister().read(GlobalConfigTO.class, file);
		}
		catch (Exception ex) {
			throw new ConfigException("User config file cannot be read or is invalid: " + file, ex);
		}
	}

	public Map<String, String> getSystemProperties() {
		return systemProperties;
	}
}
