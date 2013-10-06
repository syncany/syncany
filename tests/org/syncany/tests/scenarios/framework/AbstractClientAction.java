package org.syncany.tests.scenarios.framework;

import java.io.File;
import java.io.FileFilter;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.tests.scenarios.AllFilePossibilitiesScenarioTest;
import org.syncany.tests.util.TestClient;

public abstract class AbstractClientAction implements Executable {
	protected static final Logger logger = Logger.getLogger(AllFilePossibilitiesScenarioTest.class.getSimpleName());
	protected TestClient client;	
	protected Map<String, Object> state;
	
	protected File pickFile(int prndNum) throws Exception {
		return pickFileOrFolder(prndNum, new FileFilter() {				
			@Override
			public boolean accept(File file) {
				return file.isFile();
			}
		});
	}
	
	protected File pickFolder(int prndNum) throws Exception {
		return pickFileOrFolder(prndNum, new FileFilter() {				
			@Override
			public boolean accept(File file) {
				return file.isDirectory();
			}
		});
	}
	
	protected File pickFileOrFolder(int prndNum, FileFilter filter) throws Exception {
		int pickFileIndex = Math.abs(prndNum % (client.getLocalFiles().size()/3));			
		Iterator<File> fileIterator = null;

		int i = 0, rounds = 0;
		
		while (i <= pickFileIndex && ++rounds < 100000) {		
			if (fileIterator == null || !fileIterator.hasNext()) {
				fileIterator = client.getLocalFiles().values().iterator();
			}
			
			File pickedFile = fileIterator.next();

			if (filter.accept(pickedFile)) {
				if (i == pickFileIndex) {
					return pickedFile;
				}
				
				i++;					
			}
		}
		
		throw new Exception("Pick file should always return a file (rounds = "+rounds+"). Or is there no file there?");
	}
	
	protected void log(AbstractClientAction op, String logMessage) {
		logger.log(Level.INFO, op.getClass().getSimpleName()+": "+logMessage);
	}			
}
