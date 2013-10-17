package org.syncany.tests.scenarios.framework;

import java.io.File;

import org.syncany.tests.util.TestFileUtil;

public class ChangeTypeFolderToFile extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		File file = pickFolder(999);
		
		log(this, file.getAbsolutePath());
		
		TestFileUtil.deleteDirectory(file);
		TestFileUtil.createRandomFile(file, 25*1024);
	}		
}

