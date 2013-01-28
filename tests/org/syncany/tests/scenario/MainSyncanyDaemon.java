package org.syncany.tests.scenario;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.syncany.Syncany;
import org.syncany.tests.FileTestHelper;
import org.syncany.tests.TestSettings;

public class MainSyncanyDaemon {
	public MainSyncanyDaemon() {}

	public static boolean exit = false;
	private static TestSettings settings = TestSettings.getInstance();
	
	@Test
	public void initScenarioTest() throws InterruptedException, SecurityException, IOException {
		String configPath = settings.getConfigPath();
		String loggingFile = settings.getLoggingPath()+"mainDaemon.log";
		
		FileTestHelper.deleteFile(new File(loggingFile));
		
		// LOGGING
		ScenarioTestHelper.setupLogging(loggingFile);
		
		// cleanup
		ScenarioTestHelper.cleanupDirectories(configPath, true);

		//Syncany.start(configPath);

		while (!exit) {
			Thread.sleep(500);
		}
	}
}
