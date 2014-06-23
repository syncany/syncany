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
package org.syncany.tests.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.syncany.util.EnvironmentUtil;
import org.syncany.util.PidFileUtil;

public class PidFileUtilTest {		
	@Test
	public void testCreatePidFile() throws Exception {
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		File pidFile = new File(tempDir, "pidfile");
		
		PidFileUtil.createPidFile(pidFile);
		
		assertTrue(pidFile.exists());
		
		String pidStr = IOUtils.toString(new FileInputStream(pidFile));
		assertNotNull(pidStr);
		
		Integer pid = Integer.parseInt(pidStr);
		assertNotNull(pid);
		
		if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			Process grepPidProcess = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", "ps aux | grep java | grep " + pid });			
			String[] grepPidProcessOutput = IOUtils.toString(grepPidProcess.getInputStream()).split("\n");
			
			boolean processRunning = false;
			
			for (String line : grepPidProcessOutput) {
				if (!line.contains("grep")) {
					processRunning = true;
				}
			}
			
			assertTrue(processRunning);
		}
		
		assertTrue(PidFileUtil.isProcessRunning(pidFile));
		
		TestFileUtil.deleteDirectory(tempDir);
	}	
}
