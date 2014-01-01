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
package org.syncany.daemon.config;

import java.io.File;
import java.util.logging.Logger;

import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 */
@Root(name="syncany-daemon-config")
public class DaemonConfigurationTO {
	private static final Logger log = Logger.getLogger(DaemonConfigurationTO.class.getSimpleName());
	
	public static DaemonConfigurationTO load(File file) throws Exception {
		try {
			return new Persister().read(DaemonConfigurationTO.class, file);
		}
		catch (Exception ex) {
			log.warning("Application configuration file does not exist or is invalid: " + file);
			throw ex;
		}
	}

	public static Object getDefault() {
		return new DaemonConfigurationTO();
	}

	public static void store(Object data, File file) throws Exception {
		Serializer serializer = new Persister();
		serializer.write(data, file);
	}
}
