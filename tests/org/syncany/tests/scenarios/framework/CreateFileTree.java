package org.syncany.tests.scenarios.framework;

import java.io.File;
import java.io.IOException;

import org.syncany.tests.util.TestFileUtil;
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
		createSymlink(newFolder);	
		
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

	private void createSymlink(File inFolder) throws Exception {
		if (FileUtil.symlinksSupported()) {
			File targetFile = new File(inFolder+"/sym-target"+(fileNum++));
			File symlinkFile = new File(inFolder+"/symlink"+(fileNum++));
			
			TestFileUtil.createRandomFile(targetFile, 20*1024);			
			FileUtil.createSymlink(targetFile, symlinkFile);
		}
	}

	private void createFile(File inFolder) throws IOException {
		fileNum++;
		
		File newFile = new File(inFolder+"/file"+fileNum);		
		TestFileUtil.createRandomFile(newFile, fileNum*100);
	}
}
