package org.syncany.tests.scenarios.framework;

import java.io.File;

public class CreateFolder extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		File inFolder = pickFolder(hashCode());
		File folder = client.getLocalFile(inFolder+"/newFolder-"+Math.random());
		
		log(this, folder.getAbsolutePath());
		
		folder.mkdir();
	}		
}
