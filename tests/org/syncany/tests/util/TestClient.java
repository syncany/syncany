package org.syncany.tests.util;

import java.io.File;
import java.io.IOException;

import org.syncany.Client;
import org.syncany.config.Config;
import org.syncany.connection.plugins.Connection;

public class TestClient extends Client {
	public TestClient(String machineName, Connection connection) throws Exception {
		Config testConfig = TestConfigUtil.createTestLocalConfig(machineName, connection);
		testConfig.setMachineName(machineName);
		
		this.setConfig(testConfig);
		this.createDirectories();
	}
	
	public void createNewFiles() throws IOException {
		TestFileUtil.generateRandomBinaryFileInDirectory(config.getLocalDir(), 25*1024);
	}
	
	public void createNewFile(String name) throws IOException {
		TestFileUtil.generateRandomBinaryFile(toLocalFile(name), 50*1024);
	}
	
	public void moveFile(String fileFrom, String fileTo) throws Exception {
		boolean moveSuccess = toLocalFile(fileFrom).renameTo(toLocalFile(fileTo));
		
		if (!moveSuccess) {
			throw new Exception("Move failed: "+fileFrom+" --> "+fileTo);
		}
	}	

	public void updateFile(String name) throws IOException {
		TestFileUtil.changeRandomPartOfBinaryFile(toLocalFile(name), 0.5, 1*1024);		
	}	
	
	public void cleanup() {
		TestConfigUtil.deleteTestLocalConfigAndData(config);
	}
	
	private File toLocalFile(String name) {
		return new File(config.getLocalDir()+"/"+name);
	}
}
