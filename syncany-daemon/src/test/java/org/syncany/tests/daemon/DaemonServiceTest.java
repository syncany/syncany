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

import junit.framework.Assert;

import org.junit.Test;
import org.syncany.daemon.ServiceManager;

/**
 * 
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class DaemonServiceTest {
	
	@SuppressWarnings("serial")
	@Test
	public void testStartService() throws Exception {
		Map<String, Object> params1 = new HashMap<String, Object>(){{put("lockPort", 3338);}};
		Map<String, Object> params2 = new HashMap<String, Object>(){{put("lockPort", 3339);}};
		Map<String, Object> params3 = new HashMap<String, Object>(){{put("lockPort", 3340);}};
		Map<String, Object> params4 = new HashMap<String, Object>(){{put("lockPort", 3341);}};
		Map<String, Object> params5 = new HashMap<String, Object>(){{put("lockPort", 3342);}};

		try {
			ServiceManager.startService("daemon1", "org.syncany.daemon.Daemon", params1);
			ServiceManager.startService("daemon2", "org.syncany.daemon.Daemon", params2);
			ServiceManager.startService("daemon3", "org.syncany.daemon.Daemon", params3);
			ServiceManager.startService("daemon4", "org.syncany.daemon.Daemon", params4);
			ServiceManager.startService("daemon5", "org.syncany.daemon.Daemon", params5);
			
			Assert.assertEquals(true, ServiceManager.isServiceRunning("daemon1"));
			Assert.assertEquals(true, ServiceManager.isServiceRunning("daemon1_websocket"));
			
			Assert.assertEquals(true, ServiceManager.isServiceRunning("daemon2"));
			Assert.assertEquals(true, ServiceManager.isServiceRunning("daemon2_websocket"));
			
			Assert.assertEquals(true, ServiceManager.isServiceRunning("daemon3"));
			Assert.assertEquals(true, ServiceManager.isServiceRunning("daemon3_websocket"));
			
			Assert.assertEquals(true, ServiceManager.isServiceRunning("daemon4"));
			Assert.assertEquals(true, ServiceManager.isServiceRunning("daemon4_websocket"));
			
			Assert.assertEquals(true, ServiceManager.isServiceRunning("daemon5"));
			Assert.assertEquals(true, ServiceManager.isServiceRunning("daemon5_websocket"));
			
			ServiceManager.stopService("daemon1");

			Assert.assertEquals(false, ServiceManager.isServiceRunning("daemon1"));
			Assert.assertEquals(false, ServiceManager.isServiceRunning("daemon1_websocket"));
			
			Assert.assertEquals(true, ServiceManager.isServiceRunning("daemon2"));
			Assert.assertEquals(true, ServiceManager.isServiceRunning("daemon2_websocket"));
			
			Assert.assertEquals(true, ServiceManager.isServiceRunning("daemon3"));
			Assert.assertEquals(true, ServiceManager.isServiceRunning("daemon3_websocket"));
			
			Assert.assertEquals(true, ServiceManager.isServiceRunning("daemon4"));
			Assert.assertEquals(true, ServiceManager.isServiceRunning("daemon4_websocket"));
			
			Assert.assertEquals(true, ServiceManager.isServiceRunning("daemon5"));
			Assert.assertEquals(true, ServiceManager.isServiceRunning("daemon5_websocket"));
			
			ServiceManager.stopService("daemon2");
			ServiceManager.stopService("daemon3");
			ServiceManager.stopService("daemon4");
			ServiceManager.stopService("daemon5");
			
			Assert.assertEquals(false, ServiceManager.isServiceRunning("daemon1"));
			Assert.assertEquals(false, ServiceManager.isServiceRunning("daemon1_websocket"));
			
			Assert.assertEquals(false, ServiceManager.isServiceRunning("daemon2"));
			Assert.assertEquals(false, ServiceManager.isServiceRunning("daemon2_websocket"));
			
			Assert.assertEquals(false, ServiceManager.isServiceRunning("daemon3"));
			Assert.assertEquals(false, ServiceManager.isServiceRunning("daemon3_websocket"));
			
			Assert.assertEquals(false, ServiceManager.isServiceRunning("daemon4"));
			Assert.assertEquals(false, ServiceManager.isServiceRunning("daemon4_websocket"));
			
			Assert.assertEquals(false, ServiceManager.isServiceRunning("daemon5"));
			Assert.assertEquals(false, ServiceManager.isServiceRunning("daemon5_websocket"));
		}
		catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}