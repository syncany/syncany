package org.syncany.tests.scenarios.framework;

import java.io.File;

import org.syncany.tests.util.TestFileUtil;

public class ChangeTypeFileToFolder extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		File file = pickFile(hashCode());
		
		log(this, file.getAbsolutePath());
		
		TestFileUtil.deleteFile(file);
		file.mkdir();
	}		
}