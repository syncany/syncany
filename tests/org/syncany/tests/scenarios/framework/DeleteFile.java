package org.syncany.tests.scenarios.framework;

import java.io.File;

public class DeleteFile extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		File file = pickFile(31232);
		
		log(this, file.getAbsolutePath());		
		file.delete();
	}		
}