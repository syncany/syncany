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
package org.syncany.tests.connection.plugins.local;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.syncany.tests.connection.plugins.AbstractTransferManagerTest;
import org.syncany.tests.util.TestFileUtil;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 */
public class LocalTransferManagerPluginTest extends AbstractTransferManagerTest {
	private File tempRepoPath;
	private Map<String, String> localPluginSettings;

	@Override
	public void setUp() throws Exception {
		super.setUp();

		tempRepoPath = TestFileUtil.createTempDirectoryInSystemTemp();
		tempRepoPath.mkdir();

		localPluginSettings = new HashMap<String, String>();
		localPluginSettings.put("path", tempRepoPath.getAbsolutePath());
	}

	@Override
	public void tearDown() {
		TestFileUtil.deleteDirectory(tempRepoPath);
		super.tearDown();
	}

	@Override
	public Map<String, String> createPluginSettings() {
		return localPluginSettings;
	}

	@Override
	public String getPluginId() {
		return "local";
	}
}
