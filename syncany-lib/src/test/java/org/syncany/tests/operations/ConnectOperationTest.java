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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Random;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.config.to.ConfigTO;
import org.syncany.operations.ConnectOperation;
import org.syncany.operations.ConnectOperation.ConnectOperationOptions;
import org.syncany.operations.InitOperation;
import org.syncany.operations.InitOperation.InitOperationOptions;
import org.syncany.operations.InitOperation.InitOperationResult;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

/**
 * This test goes through the creation of a local repo and verifies
 * that the repo can be connected to.
 * @author Pim Otte
 *
 */
public class ConnectOperationTest {
	
	@Test
	public void testConnectOperation() throws Exception {	
		InitOperationOptions operationOptions = TestConfigUtil.createTestInitOperationOptions("A");
		
		InitOperation op = new InitOperation(operationOptions, null);
		InitOperationResult res = op.execute();
		File localDir = TestFileUtil.createTempDirectoryInSystemTemp(TestConfigUtil.createUniqueName("client-B", operationOptions));;
		ConnectOperationOptions connectOperationOptions = new ConnectOperationOptions();
		ConfigTO connConfigTO = operationOptions.getConfigTO();
		connConfigTO.setMachineName("client-B"+ Math.abs(new Random().nextInt()));
		connConfigTO.setMasterKey(null);
		connectOperationOptions.setConfigTO(connConfigTO);
		connectOperationOptions.setPassword(operationOptions.getPassword());
		connectOperationOptions.setLocalDir(localDir);
		ConnectOperation connOp = new ConnectOperation(connectOperationOptions, null);
		connOp.execute();
		
		File localConnectDir = new File(localDir, ".syncany");
		
		//Test the existance of generated link
		String link = res.getGenLinkResult().getShareLink();
		assertNotNull(link);
		
		//Test the local folder		
		assertTrue((new File(localConnectDir, Config.DIR_DATABASE).exists()));
		assertTrue((new File(localConnectDir, Config.DIR_CACHE).exists()));
		assertTrue((new File(localConnectDir, Config.FILE_CONFIG).exists()));
		assertTrue((new File(localConnectDir, Config.DIR_LOG).exists()));
		assertTrue((new File(localConnectDir, Config.FILE_REPO).exists()));
		assertEquals((new File(localConnectDir, Config.FILE_MASTER).exists()), TestConfigUtil.getCrypto());
		
		File repoDir = new File(operationOptions.getConfigTO().getConnectionTO().getSettings().get("path"));
		TestFileUtil.deleteDirectory(repoDir);
		TestFileUtil.deleteDirectory(localConnectDir);
		TestFileUtil.deleteDirectory(operationOptions.getLocalDir());
	}
}
