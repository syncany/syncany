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

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.syncany.cli.CommandLineClient;
import org.syncany.tests.unit.util.TestCliUtil;
import org.syncany.tests.util.TestAssertUtil;

public class PluginCommandTest {	
	@Test
	public void testPluginListLocalOnly() throws Exception {
		String[] cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"plugin", "list", "--local-only"
		}));

		assertTrue(cliOut.length >= 3);		
		TestAssertUtil.assertRegexInLines("\\| Name", cliOut);
		TestAssertUtil.assertRegexInLines("local\\s+\\|\\s+Local", cliOut);
	}
	
	@Test
	public void testPluginListRemoteOnly() throws Exception {		
		String[] cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"plugin", "list", "--remote-only"
		}));

		assertTrue(cliOut.length >= 6);		
		TestAssertUtil.assertRegexInLines("^sftp\\s+\\|\\s+", cliOut);
		TestAssertUtil.assertRegexInLines("^s3\\s+\\|\\s+", cliOut);
		TestAssertUtil.assertRegexInLines("^webdav\\s+\\|\\s+", cliOut);
		TestAssertUtil.assertRegexInLines("^ftp\\s+\\|\\s+", cliOut);
	}
}
