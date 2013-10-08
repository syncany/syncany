package org.syncany.tests.scenarios.framework;

import java.io.File;
import java.io.FileFilter;

import org.syncany.tests.util.TestFileUtil;
import org.syncany.util.FileUtil;

public class ChangeTypeSymlinkWithTargetFileToFolder extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		if (!FileUtil.symlinksSupported()) {
			return; // no symbolic links on Windows
		}
		
		File symlinkFile = pickFileOrFolder(hashCode(), new FileFilter() {
			@Override
			public boolean accept(File file) {
				return FileUtil.isSymlink(file) && file.isFile();
			}			
		});		
		
		log(this, symlinkFile.getAbsolutePath());
		
		symlinkFile.delete();
		
		symlinkFile.mkdir();
		TestFileUtil.createRandomFile(new File(symlinkFile+"/new-file"+Math.random()), 19*1024);
	}		
}	
