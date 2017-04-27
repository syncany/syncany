/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.integration.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;
import org.syncany.Client;
import org.syncany.cli.CommandLineClient;
import org.syncany.tests.unit.util.TestCliUtil;

public class UpdateCommandTest {		
	@Test
	public void testUpdateCheckOutdated() throws Exception {
		Properties originalAppProperties = (Properties) Client.getApplicationProperties().clone();
		
		Client.getApplicationProperties().setProperty("applicationVersion", "0.0.1");
		Client.getApplicationProperties().setProperty("applicationVersionFull", "0.0.1-alpha+SNAPSHOT.UNITTESTS");
		
		String[] cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] { 
			"update",
			"check", 
		}));
		
		assertEquals(4, cliOut.length);
		assertTrue(cliOut[2].contains("new version is available"));
		assertTrue(cliOut[3].contains("ownload"));
		
		Client.getApplicationProperties().setProperty("applicationVersion", originalAppProperties.getProperty("applicationVersion"));
		Client.getApplicationProperties().setProperty("applicationVersionFull", originalAppProperties.getProperty("applicationVersionFull"));		
	}	
	
	@Test
	public void testUpdateCheckUpToDate() throws Exception {
		Properties originalAppProperties = (Properties) Client.getApplicationProperties().clone();
		
		Client.getApplicationProperties().setProperty("applicationVersion", "13.3.7-alpha");
		Client.getApplicationProperties().setProperty("applicationVersionFull", "13.3.7-alpha+SNAPSHOT.UNITTESTS");
		
		String[] cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] { 
			"update",
			"check", 
		}));	
		
		assertEquals(3, cliOut.length);
		assertTrue(cliOut[2].contains("Up to date"));

		Client.getApplicationProperties().setProperty("applicationVersion", originalAppProperties.getProperty("applicationVersion"));
		Client.getApplicationProperties().setProperty("applicationVersionFull", originalAppProperties.getProperty("applicationVersionFull"));		
	}	
}
