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
package org.syncany.connection.plugins.ftp;

import junit.framework.Assert;

import org.junit.Test;
import org.syncany.connection.plugins.TransferManager.StorageTestResult;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class FtpTransferManagerTest {
	private final static String SANDBOX = "XXX";
	private final static String USERNAME = "XXX";
	private final static String PASSWORD = "XXX";
	private final static String HOST = "XXX";
	@Test
	public void testFtpTransferManager() {
		Assert.assertEquals(StorageTestResult.REPO_ALREADY_EXISTS, test(SANDBOX + "repoValid"));
		Assert.assertEquals(StorageTestResult.NO_REPO_LOCATION_EMPTY_PERMISSIONS_OK, test(SANDBOX + "repoEmpty"));
		Assert.assertEquals(StorageTestResult.NO_REPO_LOCATION_EMPTY_PERMISSIONS_KO, test(SANDBOX + "repoEmptyKO"));
		Assert.assertEquals(StorageTestResult.NO_REPO_LOCATION_NOT_EMPTY, test(SANDBOX + "notEmpty"));
		Assert.assertEquals(StorageTestResult.NO_REPO_PERMISSIONS_OK, test(SANDBOX + "repoNewPermOk"));
		Assert.assertEquals(StorageTestResult.NO_REPO_PERMISSIONS_OK, test(SANDBOX + "repoNewPermOk/new/b"));
		Assert.assertEquals(StorageTestResult.NO_REPO_PERMISSIONS_KO, test("/root/notAllowed"));
		Assert.assertEquals(StorageTestResult.INVALID_PARAMETERS, test("unknownhost", "/root/notAllowed"));
	}
		
	public StorageTestResult test(String host, String path){
		FtpConnection cnx = con(host);
		cnx.setPath(path);
		return cnx.createTransferManager().test();
	}
	
	public StorageTestResult test(String path){
		FtpConnection cnx = con();
		cnx.setPath(path);
		return cnx.createTransferManager().test();
	}
	
	public FtpConnection con(){
		return con(HOST);
	}
	
	public FtpConnection con(String host){
		FtpConnection connection = new FtpConnection();
		connection.setHostname(host);
		connection.setPort(21);
		connection.setUsername(USERNAME);
		connection.setPassword(PASSWORD);
		return connection;
	}
}
