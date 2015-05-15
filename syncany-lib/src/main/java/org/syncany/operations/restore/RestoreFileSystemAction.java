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
package org.syncany.operations.restore;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.FileUtils;
import org.syncany.config.Config;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.MemoryDatabase;
import org.syncany.operations.down.actions.ChecksumMismatchException;
import org.syncany.operations.down.actions.FileCreatingFileSystemAction;
import org.syncany.util.NormalizedPath;

public class RestoreFileSystemAction extends FileCreatingFileSystemAction {
	private String relativeTargetPath;

	public RestoreFileSystemAction(Config config, FileVersion fileVersion, String relativeTargetPath) {
		super(config, new MemoryDatabase(), null, fileVersion);
		this.relativeTargetPath = relativeTargetPath;
	}

	@Override
	public RestoreFileSystemActionResult execute() throws IOException {
		if (fileVersion2.getType() == FileType.FOLDER) {
			throw new IOException("Cannot restore folders.");
		}
		else if (fileVersion2.getType() == FileType.SYMLINK) {
			throw new IOException("Not yet implemented.");
		}
		else {
			if (fileVersion2.getStatus() == FileStatus.DELETED) {
				throw new IOException("Cannot restore version marked DELETED. Try previous version.");
			}

			// Assemble file to cache
			File cacheFile;
			try {
				cacheFile = assembleFileToCache(fileVersion2);
			}
			catch (NoSuchAlgorithmException | ChecksumMismatchException e) {
				throw new IOException(e);
			}

			// Find target path & folder
			NormalizedPath targetPath = findTargetPath();
			NormalizedPath targetFolder = targetPath.getParent();

			// Create folder (if necessary) and move file
			if (!targetFolder.toFile().isDirectory()) {
				targetFolder.toFile().mkdirs();
			}

			FileUtils.moveFile(cacheFile, targetPath.toFile());

			return new RestoreFileSystemActionResult(targetPath.toFile());
		}
	}

	private NormalizedPath findTargetPath() throws IOException {
		NormalizedPath targetPath = null;

		if (relativeTargetPath == null) {
			String restoredSuffix = "restored version " + fileVersion2.getVersion();
			targetPath = new NormalizedPath(config.getLocalDir(), fileVersion2.getPath()).withSuffix(restoredSuffix, false);
		}
		else {
			targetPath = new NormalizedPath(config.getLocalDir(), relativeTargetPath);
		}

		return targetPath;
	}
}
