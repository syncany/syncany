package org.syncany.tests.scenarios.framework;

import java.io.File;

import org.apache.commons.io.FileUtils;

public class MoveFolderWithinFolder extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		File fromFolder = pickFolder(hashCode());
		File toFolder = new File(fromFolder+"-ren"+fromFolder.hashCode());
		
		log(this, fromFolder+" -> "+toFolder);
		FileUtils.moveDirectory(fromFolder, toFolder);
	}		
}

