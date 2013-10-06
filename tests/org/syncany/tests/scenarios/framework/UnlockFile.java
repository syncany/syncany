package org.syncany.tests.scenarios.framework;

import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

public class UnlockFile extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		Object randomAccessFileObj = state.get(LockFile.STATE_KEY_RANDOM_ACCESS_FILE);
		Object fileLockObj = state.get(LockFile.STATE_KEY_FILE_LOCK);
		
		if (randomAccessFileObj == null || fileLockObj == null) {
			throw new Exception("File must be locked first before trying to unlock");
		}
		
		RandomAccessFile randomAccessFile = (RandomAccessFile) randomAccessFileObj;
		FileLock fileLock = (FileLock) fileLockObj;
		
		//log(this, randomAccessFile.getAbsolutePath());
		fileLock.release();
		randomAccessFile.close();		
	}		
}
