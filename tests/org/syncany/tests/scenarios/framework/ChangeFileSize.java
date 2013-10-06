package org.syncany.tests.scenarios.framework;

import java.io.File;
import java.io.RandomAccessFile;

public class ChangeFileSize extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		File file = pickFile(hashCode());
		
		log(this, file.getAbsolutePath());
		
		RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
		randomAccessFile.seek(randomAccessFile.length());
		randomAccessFile.writeBytes("added some bytes");
		randomAccessFile.close();
	}		
}	