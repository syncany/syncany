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
package org.syncany.tests.daemon;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.syncany.daemon.ServiceManager;

/**
 * 
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class DaemonServiceTest {
	
	@Test
	public void testStartService() throws Exception {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("lockPort", 3338);

		try {
			ServiceManager.startService("org.syncany.daemon.Daemon",params);
		}
		catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}