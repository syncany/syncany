package org.syncany.tests.scenarios.framework;

import java.io.File;
import java.util.Random;

public class ChangeLastModifiedDate extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		File file = pickFile(1221);
		
		log(this, file.getAbsolutePath());
		file.setLastModified(System.currentTimeMillis()+1000*(1+new Random().nextInt(999)));
	}		
}