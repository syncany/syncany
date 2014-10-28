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
package org.syncany.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.to.DaemonConfigTO;
import org.syncany.config.to.FolderTO;
import org.syncany.config.to.UserTO;
import org.syncany.util.FileUtil;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class DaemonConfigHelper {
	private static final Logger logger = Logger.getLogger(DaemonConfigHelper.class.getSimpleName());

	public static UserTO getFirstDaemonUser(DaemonConfigTO daemonConfig) {
		List<UserTO> users = readWebSocketServerUsers(daemonConfig);
		
		if (users.size() > 0) {
			return users.get(0);
		}
		else {
			return null;
		}
	}

	private static List<UserTO> readWebSocketServerUsers(DaemonConfigTO daemonConfigTO) {
		List<UserTO> users = daemonConfigTO.getUsers();

		if (users == null) {
			users = new ArrayList<UserTO>();
		}

		// Add CLI credentials
		if (daemonConfigTO.getPortTO() != null) {
			users.add(daemonConfigTO.getPortTO().getUser());
		}

		return users;
	}
	
	public static boolean addToDaemonConfig(File localDir) {
		File daemonConfigFile = new File(UserConfig.getUserConfigDir(), UserConfig.DAEMON_FILE);
		
		if (daemonConfigFile.exists()) {
			try {
				DaemonConfigTO daemonConfigTO = DaemonConfigTO.load(daemonConfigFile);
				String localDirPath = FileUtil.getCanonicalFile(localDir).getAbsolutePath();
				
				// Check if folder already exists
				boolean folderExists = false;
				
				for (FolderTO folderTO : daemonConfigTO.getFolders()) {
					if (localDirPath.equals(folderTO.getPath())) {
						folderExists = true;
						break;
					}
				}
				
				// Add to config if it's not already in there
				if (!folderExists) {					
					logger.log(Level.INFO, "Adding folder to daemon config: " + localDirPath + ", and saving config at " + daemonConfigFile);

					daemonConfigTO.getFolders().add(new FolderTO(localDirPath));							
					DaemonConfigTO.save(daemonConfigTO, daemonConfigFile);
					
					return true;
				}				
			}
			catch (Exception e) {
				logger.log(Level.WARNING, "Adding folder to daemon failed. Ignoring.");
			}

			return false;
		}
		else {
			FolderTO localDirFolderTO = new FolderTO(localDir.getAbsolutePath());			
			UserConfig.createAndWriteDaemonConfig(daemonConfigFile, Arrays.asList(new FolderTO[] { localDirFolderTO }));
			
			return true;
		}
	}
}
