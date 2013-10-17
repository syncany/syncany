package org.syncany.tests.scenarios.framework;

import java.io.File;

import org.syncany.tests.util.TestFileUtil;
import org.syncany.util.FileUtil;

public class ChangeTypeFolderToSymlinkWithNonExistingTarget extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		if (!FileUtil.symlinksSupported()) {
			return; // no symbolic links on Windows
		}
		
		File file = pickFolder(1811);
		
		log(this, file.getAbsolutePath());
		
		TestFileUtil.deleteDirectory(file);
		FileUtil.createSymlink("/does/not/exist", file);
	}		
}

