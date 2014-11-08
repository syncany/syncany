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
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.config.to.ConfigTO;
import org.syncany.operations.init.ConnectOperation;
import org.syncany.operations.init.ConnectOperationOptions;
import org.syncany.operations.init.ConnectOperationOptions.ConnectOptionsStrategy;
import org.syncany.operations.init.ConnectOperationResult;
import org.syncany.operations.init.ConnectOperationResult.ConnectResultCode;
import org.syncany.operations.init.InitOperation;
import org.syncany.operations.init.InitOperationOptions;
import org.syncany.operations.init.InitOperationResult;
import org.syncany.plugins.local.LocalTransferSettings;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This test goes through the creation of a local repo and verifies
 * that the repo can be connected to.
 *
 * @author Pim Otte
 */
public class ConnectOperationTest {
	@Test
	public void testConnectOperationSuccess() throws Exception {
		// A.init()
		InitOperationOptions initOperationOptionsA = TestConfigUtil.createTestInitOperationOptions("A");
		InitOperation initOperationA = new InitOperation(initOperationOptionsA, null);

		InitOperationResult initOperationResultA = initOperationA.execute();

		String connectLinkA = initOperationResultA.getGenLinkResult().getShareLink();
		assertNotNull(connectLinkA);

		// B.connect()
		File localDirB = TestFileUtil.createTempDirectoryInSystemTemp(TestConfigUtil.createUniqueName("clientB", initOperationOptionsA));
		File localConnectDirB = new File(localDirB, Config.DIR_APPLICATION);

		ConfigTO connectionConfigToB = initOperationOptionsA.getConfigTO();
		connectionConfigToB.setMachineName("clientB" + Math.abs(new Random().nextInt()));
		connectionConfigToB.setMasterKey(null);

		ConnectOperationOptions connectOperationOptionsB = new ConnectOperationOptions();
		connectOperationOptionsB.setStrategy(ConnectOptionsStrategy.CONNECTION_TO);
		connectOperationOptionsB.setConfigTO(connectionConfigToB);
		connectOperationOptionsB.setPassword(initOperationOptionsA.getPassword());
		connectOperationOptionsB.setLocalDir(localDirB);

		ConnectOperation connectOperationB = new ConnectOperation(connectOperationOptionsB, null);
		ConnectOperationResult connectOperationResultB = connectOperationB.execute();

		assertEquals(ConnectResultCode.OK, connectOperationResultB.getResultCode());
		assertTrue(new File(localConnectDirB, Config.DIR_DATABASE).exists());
		assertTrue(new File(localConnectDirB, Config.DIR_CACHE).exists());
		assertTrue(new File(localConnectDirB, Config.FILE_CONFIG).exists());
		assertTrue(new File(localConnectDirB, Config.DIR_LOG).exists());
		assertTrue(new File(localConnectDirB, Config.FILE_REPO).exists());
		assertEquals(new File(localConnectDirB, Config.FILE_MASTER).exists(), TestConfigUtil.getCrypto());

		File repoDir = ((LocalTransferSettings) initOperationOptionsA.getConfigTO().getTransferSettings()).getPath();

		// Tear down
		TestFileUtil.deleteDirectory(repoDir);
		TestFileUtil.deleteDirectory(localConnectDirB);
		TestFileUtil.deleteDirectory(initOperationOptionsA.getLocalDir());
	}

	@Test
	public void testConnectOperationFailureNoConnection() throws Exception {
		// A.init()
		InitOperationOptions initOperationOptionsA = TestConfigUtil.createTestInitOperationOptions("A");
		InitOperation initOperationA = new InitOperation(initOperationOptionsA, null);

		InitOperationResult initOperationResultA = initOperationA.execute();

		String connectLinkA = initOperationResultA.getGenLinkResult().getShareLink();
		assertNotNull(connectLinkA);

		// B.connect()
		File localDirB = TestFileUtil.createTempDirectoryInSystemTemp(TestConfigUtil.createUniqueName("client-B", initOperationOptionsA));
		File localConnectDirB = new File(localDirB, Config.DIR_APPLICATION);

		ConfigTO connectionConfigToB = initOperationOptionsA.getConfigTO();
		((LocalTransferSettings) connectionConfigToB.getTransferSettings()).setPath(new File("/does/not/exist")); // <<< Point to non-existing repo
		connectionConfigToB.setMachineName("client-B" + Math.abs(new Random().nextInt()));
		connectionConfigToB.setMasterKey(null);

		ConnectOperationOptions connectOperationOptionsB = new ConnectOperationOptions();
		connectOperationOptionsB.setStrategy(ConnectOptionsStrategy.CONNECTION_TO);
		connectOperationOptionsB.setConfigTO(connectionConfigToB);
		connectOperationOptionsB.setPassword(initOperationOptionsA.getPassword());
		connectOperationOptionsB.setLocalDir(localDirB);

		ConnectOperation connectOperationB = new ConnectOperation(connectOperationOptionsB, null);
		ConnectOperationResult connectOperationResultB = connectOperationB.execute();

		assertEquals(ConnectResultCode.NOK_TEST_FAILED, connectOperationResultB.getResultCode());
		assertFalse(new File(localConnectDirB, Config.DIR_DATABASE).exists());
		assertFalse(new File(localConnectDirB, Config.DIR_CACHE).exists());
		assertFalse(new File(localConnectDirB, Config.FILE_CONFIG).exists());
		assertFalse(new File(localConnectDirB, Config.DIR_LOG).exists());
		assertFalse(new File(localConnectDirB, Config.FILE_REPO).exists());

		File repoDir = ((LocalTransferSettings) initOperationOptionsA.getConfigTO().getTransferSettings()).getPath();

		// Tear down
		TestFileUtil.deleteDirectory(repoDir);
		TestFileUtil.deleteDirectory(localConnectDirB);
		TestFileUtil.deleteDirectory(initOperationOptionsA.getLocalDir());
	}

	@Test
	@Ignore
	public void testConnectOperationFailureInvalidRepo() throws Exception {
		// TODO [low] Write this test; ConnectResultCode.NOK_INVALID_REPO is never returned
	}
}
