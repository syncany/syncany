package org.syncany.tests.scenarios.framework;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

public class LockFile extends AbstractClientAction {
	public static final String STATE_KEY_RANDOM_ACCESS_FILE = "FileLock.randomAccessFile";
	public static final String STATE_KEY_FILE_LOCK = "FileLock.fileLock";
	
	@Override
	public void execute() throws Exception {
		if (state.containsKey("lockedFile")) {
			throw new Exception("Currently only one file can be locked at a time.");
		}
		
		File file = pickFile(hashCode());
		
		log(this, file.getAbsolutePath());
		
		RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
		FileLock fileLock = randomAccessFile.getChannel().lock();
		
		state.put(STATE_KEY_RANDOM_ACCESS_FILE, randomAccessFile);
		state.put(STATE_KEY_FILE_LOCK, fileLock);
		
		// Do not (!) close this file
	}		
}
