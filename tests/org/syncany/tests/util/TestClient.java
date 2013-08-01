package org.syncany.tests.util;

import java.io.File;
import java.io.IOException;

import org.syncany.Client;

public class TestClient extends Client {
	public TestClient() throws Exception {
		this.setConfig(TestConfigUtil.createTestLocalConfig());
		this.createDirectories();
	}
	
	public void createNewFiles() throws IOException {
		TestFileUtil.generateRandomBinaryFileInDirectory(config.getLocalDir(), 25*1024);
	}
	
	public void createNewFile(String name) throws IOException {
		TestFileUtil.generateRandomBinaryFile(toLocalFile(name), 50*1024);
	}
	
	public void moveLocalFile(String fileFrom, String fileTo) throws Exception {
		boolean moveSuccess = toLocalFile(fileFrom).renameTo(toLocalFile(fileTo));
		
		if (!moveSuccess) {
			throw new Exception("Move failed: "+fileFrom+" --> "+fileTo);
		}
	}
	
	public void cleanup() {
		TestConfigUtil.deleteTestLocalConfigAndData(config);
	}
	
	private File toLocalFile(String name) {
		return new File(config.getLocalDir()+"/"+name);
	}
}
