package org.syncany.tests.scenarios.framework;

import java.io.File;

import org.syncany.util.FileUtil;

public class CreateSymlinkToFile extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		if (!FileUtil.symlinksSupported()) {
			return; // no symbolic links on Windows
		}
		
		File inFolder = pickFolder(2311);
		File symlinkFile = new File(inFolder+"/newFile-"+Math.random());
		
		log(this, symlinkFile.getAbsolutePath());
		
		FileUtil.createSymlink("/etc/hosts", symlinkFile);
	}		
}	
