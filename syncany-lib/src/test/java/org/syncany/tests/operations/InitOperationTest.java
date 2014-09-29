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
package org.syncany.tests.operations;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.operations.init.InitOperation;
import org.syncany.operations.init.InitOperationOptions;
import org.syncany.operations.init.InitOperationResult;
import org.syncany.plugins.local.LocalTransferSettings;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.unreliable_local.UnreliableLocalTransferSettings;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This test goes through the creation of a local repo and verifies
 * the existence of files/folders as well as the connect link.
 *
 * @author Pim Otte
 */
public class InitOperationTest {
	private static final Logger logger = Logger.getLogger(InitOperation.class.getSimpleName());

	@Test
	public void testInitOperation() throws Exception {
		InitOperationOptions operationOptions = TestConfigUtil.createTestInitOperationOptions("A");
		InitOperation op = new InitOperation(operationOptions, null);
		InitOperationResult res = op.execute();
		File repoDir = ((LocalTransferSettings) operationOptions.getConfigTO().getTransferSettings()).getPath();
		File localDir = new File(operationOptions.getLocalDir(), ".syncany");

		// Test the repository
		assertTrue((new File(repoDir, "databases").exists()));
		assertTrue((new File(repoDir, "syncany").exists()));
		assertTrue((new File(repoDir, "multichunks").exists()));
		assertEquals((new File(repoDir, "master").exists()), TestConfigUtil.getCrypto());

		// Test the local folder
		assertTrue((new File(localDir, Config.DIR_DATABASE).exists()));
		assertTrue((new File(localDir, Config.DIR_CACHE).exists()));
		assertTrue((new File(localDir, Config.FILE_CONFIG).exists()));
		assertTrue((new File(localDir, Config.DIR_LOG).exists()));
		assertTrue((new File(localDir, Config.FILE_REPO).exists()));
		assertEquals((new File(localDir, Config.FILE_MASTER).exists()), TestConfigUtil.getCrypto());

		// Test the existance of generated link
		String link = res.getGenLinkResult().getShareLink();
		assertNotNull(link);

		TestFileUtil.deleteDirectory(repoDir);
		TestFileUtil.deleteDirectory(operationOptions.getLocalDir());
	}

	@Test
	public void testFaultyInitOperation() throws Exception {
		// Create an unreliable connection
		List<String> failingOperationsPattern = Lists.newArrayList("rel=1.*op=upload");
		InitOperationOptions operationOptions = TestConfigUtil.createTestUnreliableInitOperationOptions("A", failingOperationsPattern);
		InitOperation op = new InitOperation(operationOptions, null);

		File repoDir = ((UnreliableLocalTransferSettings) operationOptions.getConfigTO().getTransferSettings()).getPath();
		File localDir = new File(operationOptions.getLocalDir(), ".syncany");

		try {
			op.execute();
		}
		catch (StorageException e) {
			logger.log(Level.INFO, "This operation failed because of the unreliable connection.");
		}

		// The local directory should not exist, since the uploading of the repo file fails
		// so the local directories should be removed
		assertFalse(localDir.exists());
		// Tear down
		TestFileUtil.deleteDirectory(repoDir);
		TestFileUtil.deleteDirectory(operationOptions.getLocalDir());
	}
}
