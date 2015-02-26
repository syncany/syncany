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
package org.syncany.tests.integration.scenarios.framework;

import java.io.File;
import java.io.FileFilter;

import org.apache.commons.io.FileUtils;
import org.syncany.util.FileUtil;

public class MoveFolderToOtherFolder extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		final File fromFolder = pickFolder(1212);
		final File toFolder = pickFileOrFolder(72178, new FileFilter() {				
			@Override
			public boolean accept(File file) {
				return file.isDirectory() && !FileUtil.isSymlink(file) 
					&& !file.getAbsolutePath().startsWith(fromFolder.getAbsolutePath());
			}
		});
		
		log(this, fromFolder+" -> "+toFolder);
		FileUtils.moveDirectoryToDirectory(fromFolder, toFolder, false);
	}		
}
