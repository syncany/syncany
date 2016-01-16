/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.integration.scenarios.framework;

import java.io.File;
import java.io.IOException;

import org.syncany.tests.unit.util.TestFileUtil;
import org.syncany.util.EnvironmentUtil;
import org.syncany.util.FileUtil;

public class CreateFileTree extends AbstractClientAction {
	private int folderNum;
	private int fileNum;
	
	@Override
	public void execute() throws Exception {
		log(this, "Creating file tree");

		folderNum = 100;
		fileNum = 200;
		
		File root = client.getConfig().getLocalDir();
		
		for (int i=0; i<7; i++) {
			createFile(root);
		}
		
		for (int i=0; i<7; i++) {			
			File folder = createFolder(root);
			
			createFolder(folder);
			createFolder(folder);
		}
	}	
	
	private File createFolder(File inFolder) throws Exception {
		folderNum++;
		
		// Create folder		
		File newFolder = new File(inFolder+"/folder"+folderNum);		
		newFolder.mkdir();	
		
		// Create regular files
		int numberOfFiles = 1 + folderNum % 7;
		
		for (int i=0; i<numberOfFiles; i++) {
			createFile(newFolder);
		}
		
		// Create a symlink and its target
		createSymlinkWithTargetFile(newFolder);
		createSymlinkWithTargetFolder(newFolder);
		createSymlinkWithNonExistingTarget(newFolder);
		
		// Create a file that we don't allow ourselves access to
		createNoAccessFile(newFolder);
		
		return newFolder;
	}

	private void createNoAccessFile(File inFolder) throws IOException {
		fileNum++;
		
		File accessDeniedFile = new File(inFolder+"/access-denied"+fileNum);
		TestFileUtil.createRandomFile(accessDeniedFile, 50*1024);
		
		accessDeniedFile.setReadable(false, false);
	}

	private void createSymlinkWithTargetFile(File inFolder) throws Exception {
		if (EnvironmentUtil.symlinksSupported()) {
			String targetPathStr = inFolder+"/sym-target-"+(fileNum++);
			File symlinkFile = new File(inFolder+"/symlink-"+(fileNum++));
			
			TestFileUtil.createRandomFile(new File(targetPathStr), 20*1024);			
			FileUtil.createSymlink(targetPathStr, symlinkFile);
		}		
	}
	
	private void createSymlinkWithTargetFolder(File inFolder) throws Exception {
		if (EnvironmentUtil.symlinksSupported()) {
			String targetPathStr = inFolder+"/sym-target-folder-"+(fileNum++);
			File symlinkFile = new File(inFolder+"/symlink-"+(fileNum++));
			
			new File(targetPathStr).mkdir();			
			FileUtil.createSymlink(targetPathStr, symlinkFile);
		}	
	}
	
	private void createSymlinkWithNonExistingTarget(File inFolder) throws Exception {		
		if (EnvironmentUtil.symlinksSupported()) {
			String targetPathStr = inFolder+"/sym-target-does-NOT-exist-"+(fileNum++);
			File symlinkFile = new File(inFolder+"/symlink-BROKEN-"+(fileNum++));
						
			// Do NOT create target (!)
			FileUtil.createSymlink(targetPathStr, symlinkFile);
		}
	}

	private void createFile(File inFolder) throws IOException {
		fileNum++;
		
		File newFile = new File(inFolder+"/file"+fileNum);		
		TestFileUtil.createRandomFile(newFile, fileNum*100);
	}
}
