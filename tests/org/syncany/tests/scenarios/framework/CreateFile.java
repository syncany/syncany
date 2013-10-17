package org.syncany.tests.scenarios.framework;

import java.io.File;

import org.syncany.tests.util.TestFileUtil;

public class CreateFile extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		File inFolder = pickFolder(1942);
		File file = new File(inFolder+"/newFile-"+Math.random());
		
		log(this, file.getAbsolutePath());
		
		TestFileUtil.createRandomFile(file, 25*1024);
	}		
}