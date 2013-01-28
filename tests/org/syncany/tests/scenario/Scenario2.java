package org.syncany.tests.scenario;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;
import org.syncany.Syncany;
import org.syncany.config.Profile;
import org.syncany.tests.FileTestHelper;
import org.syncany.tests.TestSettings;

public class Scenario2 {

	private File rootFolder = null;
	private TestSettings settings = TestSettings.getInstance();
	private Logger logger;
	
	@Before
	public void init() throws SecurityException, IOException {
		String configPath2 = settings.getConfigPath2();
		String loggingFile = settings.getLoggingPath()+"scenario2.log";
		
		FileTestHelper.deleteFile(new File(loggingFile));
		
		// LOGGING
		logger = ScenarioTestHelper.setupLogging(loggingFile);
		log("Scenario2");
		
		// cleanup & not cleaning up repository (this is done by the main test)
		ScenarioTestHelper.cleanupDirectories(configPath2, false);
		
		//Syncany.start(configPath2);
		// FIXME XXXXXXXXXXXXXXXXXXXXXXXXXX DELETED PHILIPP
		
		rootFolder = Profile.getInstance().getRoot();
		
		log("rootfolder: "+rootFolder.getAbsolutePath());
	}

	@Test
	public void test() throws InterruptedException, IOException {
		//FileTestHelper.generateRandomBinaryFilesIn(rootFolder, 1024, 10);
		FileTestHelper.generateRandomBinaryFilesIn(rootFolder, 1024*100, 20);
		Thread.sleep(10000);
		
		FileTestHelper.moveFilesIntoSubdirectory(rootFolder, "subDir1");

		// waiting for uploading all chunks
		while(!Profile.getInstance().getUploader().isEmtpy()){
			Thread.sleep(1000);
		}
		
		Thread.sleep(60000);
	}

	private void log(String message) {
		logger.log(Level.INFO, message);
	}
}
