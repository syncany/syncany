package org.syncany.tests.scenarios.framework;

import java.io.File;

public class DeleteFile extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		File file = pickFile(hashCode());
		
		log(this, file.getAbsolutePath());		
		file.delete();
	}		
}