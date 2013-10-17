package org.syncany.tests.scenarios.framework;

import java.io.File;

public class CreateFolder extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		File inFolder = pickFolder(311);
		File folder = new File(inFolder+"/newFolder-"+Math.random());
		
		log(this, folder.getAbsolutePath());
		
		folder.mkdir();
	}		
}
