package org.syncany.tests.scenarios.framework;

import java.io.File;


public class MoveFileWithinFolder extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		File fromFile = pickFile(hashCode());
		File toFile = new File(fromFile+"-ren"+fromFile.hashCode());
		
		log(this, fromFile+" -> "+toFile);
		fromFile.renameTo(toFile);
	}		
}
