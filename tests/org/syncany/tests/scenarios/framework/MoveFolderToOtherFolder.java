package org.syncany.tests.scenarios.framework;

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
