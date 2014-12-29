/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.tests.scenarios.framework;

import java.io.File;
import java.io.FileFilter;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.tests.scenarios.AllFilePossibilitiesScenarioTest;
import org.syncany.tests.util.TestClient;
import org.syncany.util.FileUtil;

public abstract class AbstractClientAction implements Executable {
	protected static final Logger logger = Logger.getLogger(AllFilePossibilitiesScenarioTest.class.getSimpleName());
	protected TestClient client;	
	protected Map<String, Object> state;
	
	protected File pickFile(int prndNum) throws Exception {
		return pickFileOrFolder(prndNum, new FileFilter() {				
			@Override
			public boolean accept(File file) {
				return file.isFile() && !FileUtil.isSymlink(file);
			}
		});
	}
	
	protected File pickFolder(int prndNum) throws Exception {
		return pickFileOrFolder(prndNum, new FileFilter() {				
			@Override
			public boolean accept(File file) {
				return file.isDirectory() && !FileUtil.isSymlink(file);
			}
		});
	}
	
	protected File pickFileOrFolder(int prndNum, FileFilter filter) throws Exception {
		Map<String, File> localFiles = client.getLocalFiles();//ExcludeLockedAndNoRead();
		
		int pickFileIndex = Math.abs(prndNum % (localFiles.size()/3));			
		Iterator<File> fileIterator = null;

		int i = 0, rounds = 0;
		
		while (i <= pickFileIndex && ++rounds < 100000) {		
			if (fileIterator == null || !fileIterator.hasNext()) {
				fileIterator = localFiles.values().iterator();
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
