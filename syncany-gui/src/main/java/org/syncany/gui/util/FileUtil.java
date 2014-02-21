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
package org.syncany.gui.util;

import java.io.File;
import java.io.IOException;

import org.syncany.config.Config;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class FileUtil {
	public static boolean isSystemValidFolder(String folder) {
		if (folder.length() == 0)
			return false;

		try {
			new File(folder).getCanonicalPath();
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}

	public static boolean isRepositoryFolder(String folder) {
		return 
			isSystemValidFolder(folder) && 
			folderContainsFolder(folder, Config.DIR_APPLICATION);
	}

	public static boolean isExistingFolder( String folder) {
		return 
			isSystemValidFolder(folder) && 
			isFolderExisting(folder);
	}

	public static boolean isExistingAndEmptyFolder(String folder) {
		return 
			isSystemValidFolder(folder) && 
			isFolderExisting(folder) && 
			isEmptyFolder(folder);
	}

	public static boolean folderContainsFolder(String root, String folder) {
		File rootFolder = new File(root);
		File syncanyFolder = new File(root, folder);
		return rootFolder.exists() && syncanyFolder.exists();
	}

	public static boolean isEmptyFolder(String folder) {
		File f = new File(folder);
		
		if (!f.exists()){
			return true;
		}
		
		return f.list().length == 0;
	}

	public static boolean isFolderExisting(String folder) {
		File f = new File(folder);
		return f.exists();
	}
}
