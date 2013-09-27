package org.syncany.tests.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.syncany.Client;
import org.syncany.config.Config;
import org.syncany.connection.plugins.Connection;
import org.syncany.database.Database;
import org.syncany.database.DatabaseDAO;
import org.syncany.database.DatabaseXmlDAO;
import org.syncany.util.FileUtil;

public class TestClient extends Client {
	public TestClient(String machineName, Connection connection) throws Exception {
		Config testConfig = TestConfigUtil.createTestLocalConfig(machineName, connection);
		testConfig.setMachineName(machineName);
		
		this.setConfig(testConfig);
		this.createDirectories();
	}	
	
	public void createNewFiles() throws IOException {
		TestFileUtil.createRandomFilesInDirectory(config.getLocalDir(), 25*1024, 20);		
	}
	
	public void createNewFile(String name) throws IOException {
		createNewFile(name, 50*1024);
	}
	
	public void createNewFile(String name, int size) throws IOException {
		File localFile = getLocalFile(name);		
		TestFileUtil.createRandomFile(localFile, size);
	}
	
	public void createNewFolder(String name) {
		getLocalFile(name).mkdirs();		
	}	
	
	public void moveFile(String fileFrom, String fileTo) throws Exception {
		File fromLocalFile = getLocalFile(fileFrom);
		File toLocalFile = getLocalFile(fileTo);
		
		boolean moveSuccess = fromLocalFile.renameTo(toLocalFile);
		
		if (!moveSuccess) {
			throw new Exception("Move failed: "+fileFrom+" --> "+fileTo);
		}		
	}	
	
	public void copyFile(String fileFrom, String fileTo) throws IOException {
		FileUtil.copy(getLocalFile(fileFrom), getLocalFile(fileTo));		
	}

	public void changeFile(String name) throws IOException {
		TestFileUtil.changeRandomPartOfBinaryFile(getLocalFile(name));		
	}	
	
	public void deleteFile(String name) {
		getLocalFile(name).delete();		
	}	
	
	public void cleanup() {
		TestConfigUtil.deleteTestLocalConfigAndData(config);
	}
	
	public File getLocalFile(String name) {
		return new File(config.getLocalDir()+"/"+name);
	}
	
	public Map<String, File> getLocalFiles() throws FileNotFoundException {
		List<File> fileList = FileUtil.getRecursiveFileList(config.getLocalDir(), true);
		Map<String, File> fileMap = new HashMap<String, File>();
		
		for (File file : fileList) {
			fileMap.put(FileUtil.getRelativePath(config.getLocalDir(), file), file);
		}
		
		return fileMap;
	}
 
	public File getLocalDatabaseFile() {
		return config.getDatabaseFile();
	}

	public File getDirtyDatabaseFile() {
		return config.getDirtyDatabaseFile();
	}
	
	public Database loadLocalDatabase() throws IOException {
		Database db = new Database();
		
		DatabaseDAO dao = new DatabaseXmlDAO();
		dao.load(db, getLocalDatabaseFile());
		
		return db;
		
	}	
}
