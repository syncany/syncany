package org.syncany.tests.scenarios.framework;

import java.io.File;

import org.syncany.util.FileUtil;

public class ChangeTypeToSymlinkToFolder extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		if (!FileUtil.symlinksSupported()) {
			return; // no symbolic links on Windows
		}
		
		File file = pickFile(hashCode());
		
		log(this, file.getAbsolutePath());
		
		file.delete();
		FileUtil.createSymlink(new File("/etc/init.d"), file);
	}		
}	
