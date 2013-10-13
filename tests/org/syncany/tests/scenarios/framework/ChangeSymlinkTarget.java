package org.syncany.tests.scenarios.framework;

import java.io.File;
import java.io.FileFilter;

import org.syncany.util.FileUtil;

public class ChangeSymlinkTarget extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		if (!FileUtil.symlinksSupported()) {
			return; // no symbolic links on Windows
		}
		
		File symlinkFile = pickFileOrFolder(hashCode(), new FileFilter() {
			@Override
			public boolean accept(File file) {
				return FileUtil.isSymlink(file);
			}			
		});		
		
		log(this, symlinkFile.getAbsolutePath());
		
		symlinkFile.delete();
		FileUtil.createSymlink("/does/not/exist/"+Math.random(), symlinkFile);
	}		
}	
