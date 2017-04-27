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
package org.syncany.tests.unit.operations.daemon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.syncany.config.to.FolderTO;
import org.syncany.operations.daemon.DaemonOperationResult;
import org.syncany.operations.daemon.DaemonOperationResult.DaemonResultCode;

/**
 * Unit tests for the {@link DaemonOperationResult} class.
 * 
 * @author Niels Spruit
 *
 */
public class DaemonOperationResultTest {

	private DaemonOperationResult result;
	private FolderTO folder;
	private ArrayList<FolderTO> watchList;

	@Before
	public void setUp() {
		folder = new FolderTO("test");
		watchList = new ArrayList<FolderTO>();
		watchList.add(folder);
		result = new DaemonOperationResult(DaemonResultCode.OK, watchList);
	}

	@Test
	public void testGetResultCode() {
		DaemonResultCode res = result.getResultCode();
		assertNotNull(res);
		assertEquals(DaemonResultCode.OK, res);
	}

	@Test
	public void testSetResultCode() {
		result.setResultCode(DaemonResultCode.NOK);
		DaemonResultCode res = result.getResultCode();
		assertNotNull(res);
		assertEquals(DaemonResultCode.NOK, res);
	}

	@Test
	public void testGetWatchList() {
		ArrayList<FolderTO> res = result.getWatchList();
		assertNotNull(res);
		assertFalse(res.isEmpty());
		assertTrue(res.contains(folder));
	}

	@Test
	public void setWatchList() {
		ArrayList<FolderTO> newList = new ArrayList<FolderTO>();
		result.setWatchList(newList);
		ArrayList<FolderTO> res = result.getWatchList();
		assertNotNull(res);
		assertTrue(res.isEmpty());
	}

}
