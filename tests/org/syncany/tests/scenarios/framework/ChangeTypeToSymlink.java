package org.syncany.tests.scenarios.framework;

import java.io.File;

import org.syncany.tests.util.TestFileUtil;

public class ChangeTypeToSymlink extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		if (File.separatorChar == '\\') {
			return; // no symbolic links on Windows
		}
		
		File file = pickFile(hashCode());
		
		log(this, file.getAbsolutePath());
		
		file.delete();
		TestFileUtil.createSymlink(new File("/etc/hosts"), file);
	}		
}	
