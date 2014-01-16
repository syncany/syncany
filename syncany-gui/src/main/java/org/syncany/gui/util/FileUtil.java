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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

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

	public static boolean isSyncanyFolder(Shell shell, String folder) {
		return isSystemValidFolder(folder) && folderContainsFolder(folder, ".syncany");
	}

	public static boolean isExistingFolder(Shell shell, String folder) {
		return isSystemValidFolder(folder) && isFolderExisting(shell, folder);
	}

	public static boolean isExistingAndEmptyFolder(Shell shell, String folder) {
		return isSystemValidFolder(folder) && isFolderExisting(shell, folder) && isEmptyFolder(shell, folder);
	}

	public static boolean folderContainsFolder(String root, String folder) {
		File rootFolder = new File(root);
		File syncanyFolder = new File(root, folder);
		return rootFolder.exists() && syncanyFolder.exists();
	}

	public static boolean isEmptyFolder(Shell shell, String folder) {
		File f = new File(folder);
		boolean ret = f.list().length == 0;

		if (!ret) {
			MessageBox dialog = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.CANCEL);
			dialog.setText("Warning");
			dialog.setMessage("Folder is not empty, please choose an empty folder");
			dialog.open();
		}
		return ret;
	}

	public static boolean isFolderExisting(Shell shell, String folder) {
		File f = new File(folder);
		return f.exists() ? true : askCreateFolder(shell, folder);
	}

	public static boolean askCreateFolder(Shell shell, String f) {
		File localDirFile = new File(f);
		MessageBox dialog = new MessageBox(shell, SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);
		dialog.setText("Create Folder");
		dialog.setMessage(String.format("Would you like to create the folder [%s]?", localDirFile.getAbsolutePath()));

		int ret = dialog.open();

		if (ret == SWT.OK) {
			boolean mkdirSuccess = localDirFile.mkdir();

			if (mkdirSuccess) {
				return true;
			}
			return false;
		}
		return false;
	}
}
