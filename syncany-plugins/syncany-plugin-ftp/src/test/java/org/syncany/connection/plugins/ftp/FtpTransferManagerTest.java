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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager.StorageTestResult;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class FtpTransferManagerTest {
	@BeforeClass
	public static void beforeTestSetup() throws Exception {
		EmbeddedFtpServerTest.startServer();
		
		EmbeddedFtpServerTest.mkdir("newRepo", EmbeddedFtpServerTest.USER1);
		EmbeddedFtpServerTest.mkdir("nonEmptyRepo", EmbeddedFtpServerTest.USER1);
		EmbeddedFtpServerTest.mkdir("nonEmptyRepo/folder", EmbeddedFtpServerTest.USER1);
		EmbeddedFtpServerTest.mkdir("canNotCreate", EmbeddedFtpServerTest.USER2);
	}
	
	@AfterClass
	public static void stop(){
		EmbeddedFtpServerTest.stopServer();
	}
	
	@Test
	public void testFtpTransferManager() throws StorageException {
		Assert.assertEquals(StorageTestResult.NO_REPO, test("/newRepo"));
		Assert.assertEquals(StorageTestResult.NO_REPO, test("/randomRepo"));
		Assert.assertEquals(StorageTestResult.REPO_EXISTS, test("/nonEmptyRepo"));
		Assert.assertEquals(StorageTestResult.NO_REPO_CANNOT_CREATE, test("/canNotCreate/inside"));
	}
		
	public StorageTestResult test(String path) throws StorageException{
		FtpConnection cnx = workingConnection();
		cnx.setPath(path);
		return cnx.createTransferManager().test();
	}
	
	public FtpConnection workingConnection(){
		FtpConnection connection = new FtpConnection();
		connection.setHostname(EmbeddedFtpServerTest.HOST);
		connection.setPort(EmbeddedFtpServerTest.PORT);
		connection.setUsername(EmbeddedFtpServerTest.USER1);
		connection.setPassword(EmbeddedFtpServerTest.PASSWORD1);
		return connection;
	}
	
	public FtpConnection invalidConnection(){
		FtpConnection connection = new FtpConnection();
		connection.setHostname(EmbeddedFtpServerTest.HOST_WRONG);
		connection.setPort(EmbeddedFtpServerTest.PORT);
		connection.setUsername(EmbeddedFtpServerTest.USER1);
		connection.setPassword(EmbeddedFtpServerTest.PASSWORD1);
		return connection;
	}
}
