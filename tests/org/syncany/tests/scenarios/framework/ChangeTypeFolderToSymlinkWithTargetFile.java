package org.syncany.tests.scenarios.framework;

import java.io.File;

import org.syncany.tests.util.TestFileUtil;
import org.syncany.util.FileUtil;

public class ChangeTypeFolderToSymlinkWithTargetFile extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		if (!FileUtil.symlinksSupported()) {
			return; // no symbolic links on Windows
		}
		
		File file = pickFolder(1977);
		
		log(this, file.getAbsolutePath());
		
		TestFileUtil.deleteDirectory(file);
		FileUtil.createSymlink("/etc/hosts", file);
	}		
}

