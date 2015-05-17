/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.unit.operations.daemon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.operations.daemon.DaemonOperation;
import org.syncany.operations.daemon.DaemonOperationOptions;
import org.syncany.operations.daemon.DaemonOperationOptions.DaemonAction;
import org.syncany.operations.daemon.DaemonOperationResult;
import org.syncany.operations.daemon.DaemonOperationResult.DaemonResultCode;
import org.syncany.tests.unit.util.TestFileUtil;
import org.syncany.tests.util.TestDaemonUtil;

/**
 * Unit tests for the {@link DaemonOperation} class, 
 * using Mockito for mocking its instance variable options.
 * 
 * @author Niels Spruit
 */
public class DaemonOperationTest {
	private DaemonOperationOptions options;
	private DaemonOperation deamonOp;

	private static final String WATCH_ROOT_APP_FOLDER = "watch_root_folder/" + Config.DIR_APPLICATION;
	private static File tempWatchRootAppFolder;

	@BeforeClass
	public static void initialize() throws Exception {
		tempWatchRootAppFolder = TestFileUtil.createTempDirectoryInSystemTemp(WATCH_ROOT_APP_FOLDER);
		TestDaemonUtil.cleanUserConfig();
	}

	@Before
	public void setUp() throws Exception {
		options = mock(DaemonOperationOptions.class);
		deamonOp = new DaemonOperation(options);
	}

	@Test
	public void testExecuteList() throws Exception {
		when(options.getAction()).thenReturn(DaemonAction.LIST);

		DaemonOperationResult res = deamonOp.execute();
		assertNotNull(res);
		assertEquals(DaemonResultCode.OK, res.getResultCode());
		assertNotNull(res.getWatchList());
	}

	@Test
	public void testExecuteAdd() throws Exception {
		when(options.getAction()).thenReturn(DaemonAction.ADD);

		List<String> watchRoots = new ArrayList<String>();
		watchRoots.add(tempWatchRootAppFolder.getParent());
		when(options.getWatchRoots()).thenReturn(watchRoots);

		DaemonOperationResult res = deamonOp.execute();

		assertNotNull(res);
		assertEquals(DaemonResultCode.OK, res.getResultCode());
		assertNotNull(res.getWatchList());
		assertEquals(1, res.getWatchList().size());
		assertEquals(tempWatchRootAppFolder.getParentFile().getAbsolutePath(), res.getWatchList().get(0).getPath());
	}

	@Test
	public void testExecuteRemove() throws Exception {
		when(options.getAction()).thenReturn(DaemonAction.REMOVE);

		List<String> watchRoots = new ArrayList<String>();
		watchRoots.add(tempWatchRootAppFolder.getParent());
		when(options.getWatchRoots()).thenReturn(watchRoots);

		DaemonOperationResult res = deamonOp.execute();

		assertNotNull(res);
		assertEquals(DaemonResultCode.OK, res.getResultCode());
		assertNotNull(res.getWatchList());
		assertTrue(res.getWatchList().isEmpty());
	}

	@AfterClass
	public static void cleanUp() {
		// remove created folders
		TestFileUtil.deleteDirectory(tempWatchRootAppFolder.getParentFile());
	}

}
