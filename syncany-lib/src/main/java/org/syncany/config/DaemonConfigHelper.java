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
import org.syncany.crypto.CipherUtil;
import org.syncany.operations.watch.WatchOperationOptions;
import org.syncany.util.FileUtil;

/**
 * The daemon helper provides helper functions to read and/or write the
 * daemon configuration file as defined by {@link DaemonConfigTO}.
 *
 * @author Vincent Wiencek <vwiencek@gmail.com>
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
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

	/**
	 * Adds the given folder to the user-specific daemon configuration (<tt>daemon.xml</tt>).
	 *
	 * <p>The method first reads the daemon configuration, checks if the folder is already present
	 * and adds it if it is not. If no daemon config file exists, a new default config file is created
	 * via {@link #createAndWriteDefaultDaemonConfig(File)}. If the folder is already present in
	 * the current daemon config, <tt>false</tt> is returned. If an error occurs (e.g. an I/O error
	 * or an invalid XML file), a {@link ConfigException} is thrown. If the folder was successfully added,
	 * <tt>true</tt> is returned.
	 *
	 * @param localDir Absolute path of the local folder to add to the daemon config
	 * @return Returns <tt>true</tt> if the folder was successfully added to the daemon config,
	 *         <tt>false</tt> otherwise
	 * @throws ConfigException If an error occurs, e.g. an I/O error or an invalid XML file
	 */
	public static boolean addToDaemonConfig(File localDir) throws ConfigException {
		File daemonConfigFile = new File(UserConfig.getUserConfigDir(), UserConfig.DAEMON_FILE);

		if (daemonConfigFile.exists()) {
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
				daemonConfigTO.save(daemonConfigFile);

				return true;
			}
			else {
				return false;
			}
		}
		else {
			FolderTO localDirFolderTO = new FolderTO(localDir.getAbsolutePath());
			createAndWriteDaemonConfig(daemonConfigFile, Arrays.asList(new FolderTO[] { localDirFolderTO }));

			return true;
		}
	}

    public static DaemonConfigTO createAndWriteDefaultDaemonConfig(File daemonConfigFile) throws ConfigException {
		return createAndWriteDaemonConfig(daemonConfigFile, new ArrayList<FolderTO>());
	}

    public static DaemonConfigTO createAndWriteExampleDaemonConfig(File daemonConfigFile) throws ConfigException {
    	File defaultFolder = new File(System.getProperty("user.home"), UserConfig.DEFAULT_FOLDER);

    	FolderTO defaultFolderTO = new FolderTO();
    	defaultFolderTO.setPath(defaultFolder.getAbsolutePath());
    	defaultFolderTO.setWatchOptions(new WatchOperationOptions());

    	return createAndWriteDaemonConfig(daemonConfigFile, Arrays.asList(new FolderTO[] { defaultFolderTO }));
	}

    public static DaemonConfigTO createAndWriteDaemonConfig(File configFile, List<FolderTO> folders) throws ConfigException {
    	UserTO defaultUserTO = new UserTO();
		defaultUserTO.setUsername(UserConfig.USER_ADMIN);
		defaultUserTO.setPassword(CipherUtil.createRandomAlphabeticString(12));

		ArrayList<UserTO> users = new ArrayList<>();
		users.add(defaultUserTO);

		DaemonConfigTO defaultDaemonConfigTO = new DaemonConfigTO();
		defaultDaemonConfigTO.setFolders(new ArrayList<>(folders));
		defaultDaemonConfigTO.setUsers(users);

		defaultDaemonConfigTO.save(configFile);

		return defaultDaemonConfigTO;
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
}
