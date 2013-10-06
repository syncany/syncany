package org.syncany.tests.scenarios.framework;

import java.io.File;

public class ChangeFilePermissionsToXXX extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		File file = pickFile(hashCode());
		
		log(this, "NOT YET IMPLEMENTED -- "+file.getAbsolutePath());
		
	}		
}	
