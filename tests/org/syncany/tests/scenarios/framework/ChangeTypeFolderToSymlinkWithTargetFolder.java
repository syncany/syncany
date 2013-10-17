package org.syncany.tests.scenarios.framework;

import java.io.File;

import org.syncany.tests.util.TestFileUtil;
import org.syncany.util.FileUtil;

public class ChangeTypeFolderToSymlinkWithTargetFolder extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		if (!FileUtil.symlinksSupported()) {
			return; // no symbolic links on Windows
		}
		
		File file = pickFolder(18177);
		
		log(this, file.getAbsolutePath());
		
		TestFileUtil.deleteDirectory(file);
		FileUtil.createSymlink("/etc/init.d", file);
	}		
}

