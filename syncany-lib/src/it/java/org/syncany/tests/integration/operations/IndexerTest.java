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
package org.syncany.tests.integration.operations;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.syncany.tests.util.TestFileUtil;

public class IndexerTest {
	private File tempLocalSourceDir;
	private File tempLocalCacheDir;
	
	@Before
	public void setUp() throws Exception {
		File rootDir = TestFileUtil.createTempDirectoryInSystemTemp();
		
		tempLocalSourceDir = new File(rootDir+"/local");
		tempLocalSourceDir.mkdir();
		
		tempLocalCacheDir = new File(rootDir+"/cache");		
		tempLocalCacheDir.mkdir();
	}
	
	@After
	public void tearDown() {
		TestFileUtil.deleteDirectory(tempLocalSourceDir);
	}
	
	@Test
	@Ignore
	public void testDeduperWithDatabase() throws IOException {
		// TODO [high] write indexer test
	}

}
