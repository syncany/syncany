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
package org.syncany.daemon;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class ServiceManager {
	private static final Map<String, Service> services = new HashMap<String, Service>();
	
	public static void startService(String identifier, String serviceClassName, Map<String, Object> params) throws Exception {
		if (services.containsKey(identifier))
			throw new Exception("service already exists");
	
		Service service = (Service)Class.forName(serviceClassName).newInstance();
		service.setIdentifier(identifier);
		service.start(params);
		services.put(identifier, service);
	}
	
	public static void stopService(String identifier){
		Service service = services.get(identifier);
		
		if (service != null){
			service.stop();
		}
	}

	public static boolean isServiceRunning(String identifier) {
		Service service = services.get(identifier);
		
		if (service != null){
			return service.isRunning();
		}
		return false;
	}

	public static <T> T getService(String identifier, Class<T> clazz) {
		Service service = services.get(identifier);
		return clazz.cast(service);
	}
}
