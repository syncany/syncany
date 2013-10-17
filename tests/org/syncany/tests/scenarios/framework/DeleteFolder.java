package org.syncany.tests.scenarios.framework;

import java.io.File;

public class DeleteFolder extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		File folder = pickFolder(8928);
		
		log(this, folder.getAbsolutePath());		
		folder.delete();
	}		
}