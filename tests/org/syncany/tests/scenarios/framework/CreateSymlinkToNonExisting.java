package org.syncany.tests.scenarios.framework;

import java.io.File;

import org.syncany.util.FileUtil;

public class CreateSymlinkToNonExisting extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		if (!FileUtil.symlinksSupported()) {
			return; // no symbolic links on Windows
		}
		
		File inFolder = pickFolder(hashCode());
		File symlinkFile = client.getLocalFile(inFolder+"/newFile-"+Math.random());
		
		log(this, symlinkFile.getAbsolutePath());
		
		FileUtil.createSymlink(new File("/does/not/exist"), symlinkFile);
	}		
}	
