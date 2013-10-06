package org.syncany.tests.scenarios.framework;

import java.io.File;
import java.io.FileFilter;

import org.apache.commons.io.FileUtils;

public class MoveFolderToOtherFolder extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		final File fromFolder = pickFolder(hashCode());
		final File toFolder = pickFileOrFolder(hashCode()*hashCode(), new FileFilter() {				
			@Override
			public boolean accept(File file) {
				return file.isDirectory() && !file.getAbsolutePath().startsWith(fromFolder.getAbsolutePath());
			}
		});
		
		log(this, fromFolder+" -> "+toFolder);
		FileUtils.moveDirectoryToDirectory(fromFolder, toFolder, false);
	}		
}
