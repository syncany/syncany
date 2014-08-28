package org.syncany.gui.command;
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


import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import org.junit.Test;
import org.syncany.operations.daemon.messages.ListWatchesRequest;
import org.syncany.operations.daemon.messages.Request;
import org.syncany.operations.daemon.messages.Response;
import org.syncany.tests.util.TestCliUtil;
import org.syncany.tests.util.TestConfigUtil;

public class GUIClientTest {	
	private static final Logger logger = Logger.getLogger(GUIClientTest.class.getSimpleName());
	
	@Test
	public void testGuiClient() throws Exception {
		GUIClient gc = new GUIClient();
		
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnv("A", connectionSettings);
		Map<String, String> clientB = TestCliUtil.createLocalTestEnv("B", connectionSettings);

		Request req = new ListWatchesRequest();
		req.setId(Math.abs(new Random().nextInt()));
		Response response = gc.runCommand(req);
		int i = 1;
	}	
}
