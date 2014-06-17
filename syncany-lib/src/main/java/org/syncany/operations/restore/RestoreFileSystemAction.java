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
package org.syncany.operations.restore;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.syncany.config.Config;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.MemoryDatabase;
import org.syncany.operations.down.actions.FileCreatingFileSystemAction;
import org.syncany.util.NormalizedPath;

/**
 * @author pheckel
 *
 */
public class RestoreFileSystemAction extends FileCreatingFileSystemAction {
	private File targetFile;
	
	public RestoreFileSystemAction(Config config, FileVersion fileVersion, File targetFile) {
		super(config, new MemoryDatabase(), null, fileVersion);
		this.targetFile = targetFile;
	}

	@Override
	public void execute() throws Exception {
		if (fileVersion2.getType() == FileType.FOLDER) {
			throw new Exception("Cannot restore folders.");
		}
		else if (fileVersion2.getType() == FileType.SYMLINK) {
			throw new Exception("Not yt implemented.");
		}
		else {
			File cacheFile = assembleFileToCache(fileVersion2);
			
			NormalizedPath normalizedTargetPath = new NormalizedPath(config.getLocalDir(), fileVersion2.getPath());
			NormalizedPath creatableTargetPath = normalizedTargetPath.toCreatable("restored", false);
			
			FileUtils.moveFile(cacheFile, creatableTargetPath.toFile());
		}
	}

}
