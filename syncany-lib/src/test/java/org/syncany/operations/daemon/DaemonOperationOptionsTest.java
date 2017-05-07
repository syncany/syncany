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
package org.syncany.operations.daemon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.syncany.operations.daemon.DaemonOperationOptions.DaemonAction;

/**
 * Unit tests for the {@link DaemonOperationOptions} class.
 * 
 * @author Niels Spruit
 *
 */
public class DaemonOperationOptionsTest {

	private DaemonOperationOptions options;

	@Before
	public void setUp() {
		options = new DaemonOperationOptions(DaemonAction.ADD);
	}

	@Test
	public void testGetAction() {
		DaemonAction res = options.getAction();
		assertNotNull(res);
		assertEquals(DaemonAction.ADD, res);
	}

	@Test
	public void testSetAction() {
		options.setAction(DaemonAction.LIST);
		assertNotNull(options.getAction());
		assertEquals(DaemonAction.LIST, options.getAction());
	}

	@Test
	public void testGetWatchRoots() {
		options = new DaemonOperationOptions();

		List<String> res = options.getWatchRoots();
		assertNotNull(res);
		assertTrue(res.isEmpty());
	}

	@Test
	public void testSetWatchRoots() {
		options = new DaemonOperationOptions();

		List<String> watchRoots = new ArrayList<String>();
		watchRoots.add("root1");
		watchRoots.add("root2");
		options.setWatchRoots(watchRoots);

		List<String> res = options.getWatchRoots();
		assertNotNull(res);
		assertFalse(res.isEmpty());
		assertTrue(res.contains("root1"));
		assertTrue(res.contains("root2"));
	}
}
