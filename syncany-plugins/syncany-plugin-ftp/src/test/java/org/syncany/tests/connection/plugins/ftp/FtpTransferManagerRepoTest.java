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
package org.syncany.tests.connection.plugins.ftp;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager.StorageTestResult;
import org.syncany.connection.plugins.ftp.FtpConnection;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 */
public class FtpTransferManagerRepoTest {
	@BeforeClass
	public static void beforeTestSetup() throws Exception {
		EmbeddedTestFtpServer.startServer();

		EmbeddedTestFtpServer.mkdir("newRepo", EmbeddedTestFtpServer.USER1);
		EmbeddedTestFtpServer.mkdir("nonEmptyRepo", EmbeddedTestFtpServer.USER1);
		EmbeddedTestFtpServer.mkdir("nonEmptyRepo/folder", EmbeddedTestFtpServer.USER1);
		EmbeddedTestFtpServer.mkdir("canNotCreate", EmbeddedTestFtpServer.USER2);
	}

	@AfterClass
	public static void stop() {
		EmbeddedTestFtpServer.stopServer();
	}

	@Test
	public void testFtpTransferManager() throws StorageException {
		Assert.assertEquals(StorageTestResult.REPO_EXISTS_BUT_INVALID, test("/newRepo"));
		Assert.assertEquals(StorageTestResult.NO_REPO, test("/randomRepo"));
		Assert.assertEquals(StorageTestResult.REPO_EXISTS_BUT_INVALID, test("/nonEmptyRepo"));
		Assert.assertEquals(StorageTestResult.NO_REPO_CANNOT_CREATE, test("/canNotCreate/inside"));
	}

	public StorageTestResult test(String path) throws StorageException {
		FtpConnection cnx = workingConnection();
		cnx.setPath(path);
		return cnx.createTransferManager().test();
	}

	public FtpConnection workingConnection() {
		FtpConnection connection = new FtpConnection();
		connection.setHostname(EmbeddedTestFtpServer.HOST);
		connection.setPort(EmbeddedTestFtpServer.PORT);
		connection.setUsername(EmbeddedTestFtpServer.USER1);
		connection.setPassword(EmbeddedTestFtpServer.PASSWORD1);
		return connection;
	}

	public FtpConnection invalidConnection() {
		FtpConnection connection = new FtpConnection();
		connection.setHostname(EmbeddedTestFtpServer.HOST_WRONG);
		connection.setPort(EmbeddedTestFtpServer.PORT);
		connection.setUsername(EmbeddedTestFtpServer.USER1);
		connection.setPassword(EmbeddedTestFtpServer.PASSWORD1);
		return connection;
	}
}
