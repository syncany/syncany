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
package org.syncany.tests.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.syncany.config.to.DaemonConfigTO;
import org.syncany.config.to.PortTO;
import org.syncany.config.to.UserTO;
import org.syncany.crypto.CipherUtil;

/**
 * This class contains various helper methods for daemon related tests.
 * 
 * @author Pim Otte
 */
public class TestDaemonUtil {
	private static final String DAEMON_RESOURCE_PATTERN = "/org/syncany/config/to/%s";
	
	public static DaemonConfigTO loadDaemonConfig(String resourceFilename) throws Exception {
		// Load config from resource
		String fullPathResource = String.format(DAEMON_RESOURCE_PATTERN, resourceFilename);
		InputStream inputStream = DaemonConfigTO.class.getResourceAsStream(fullPathResource);
		File tempConfigFile = File.createTempFile("syncanyTemp-", "");
		tempConfigFile.deleteOnExit();
		
		try (FileOutputStream outputStream = new FileOutputStream(tempConfigFile)) {
	        IOUtils.copy(inputStream, outputStream);
	    }
		
		return DaemonConfigTO.load(tempConfigFile);
	}
	
	public static PortTO createPortTO(int port) {
		String accessToken = CipherUtil.createRandomAlphabeticString(20);
		
		UserTO cliUser = new UserTO();
		cliUser.setUsername("CLI");
		cliUser.setPassword(accessToken);
		
		PortTO portTO = new PortTO();
		portTO.setPort(port);
		portTO.setUser(cliUser);
		return portTO;
	}
}
