package org.syncany.tests.scenarios.framework;

import java.io.File;
import java.io.RandomAccessFile;


public class ChangeContentWithoutFileSize extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		File file = pickFile(hashCode());
		
		log(this, file.getAbsolutePath());
		
		RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
		randomAccessFile.writeDouble(Math.random());
		randomAccessFile.close();
	}		
}	
