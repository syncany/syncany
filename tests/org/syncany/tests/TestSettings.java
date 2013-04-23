package org.syncany.tests;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.syncany.Constants;
import org.syncany.config.ConfigTO;
import org.syncany.config.Profile;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.exceptions.EncryptionException;


public class TestSettings {
	
	private static TestSettings instance;
	
	// path to config Json
	private String configPath;
	private String configPath2;
	private String loggingPath;
	
	// main Testattributes
	private File appDir;
	private File appCacheDir;
	private File repDir;
	private File rootFolder;
	private String machineName;
	
	// for synchronization tests attributes for a "remote" syncany instance
	private File appDir2;
	private File appCacheDir2;
	private File rootFolder2;
	
	// Scenario-Tests Home-Folders
	private File home1;
	private File home2;
	
	// ChunkSize
	private int chunkSize = Constants.DEFAULT_CHUNK_SIZE;
	
	private ConfigTO settings;
	
	private TestSettings() {
		
		if (System.getProperty("os.name").contains("Mac OS X")) {
			configPath = new String("/opt/config.json");
			configPath2 = new String("/opt/config2.json");
			loggingPath = new String("/opt/");
			
			appDir = new File("/opt/syncanytest/applicationData");
			appCacheDir = new File("/opt/syncanytest/cache");
			repDir = new File("/opt/syncanytest/repfolder");
			rootFolder = new File("/opt/syncanytest/testfolder");
			machineName = "MACMachine";
			
			home1 = new File("/opt/syncanytest/");
			home2 = new File("/opt/syncanytest2/");
			
			// "remote" folders
			appDir2 = new File("/opt/syncanytest2/applicationData");
			appCacheDir2 = new File("/opt/syncanytest2/cache");
			rootFolder2 = new File("/opt/syncanytest2/testfolder");
	    	
		} else if (System.getProperty("os.name").contains("Windows")) {
			configPath = new String("D:/Syncany/SyncanyWorkspace/config.json");
			configPath2 = new String("D:/Syncany/SyncanyWorkspace/config2.json");
			loggingPath = new String("D:/Syncany/SyncanyWorkspace/");
			
			appDir = new File("D:/Syncany/Testfolder/applicationData");
			appCacheDir = new File("D:/Syncany/Testfolder/cache");
			repDir = new File("D:/Syncany/Testfolder/repository");
			rootFolder = new File("D:/Syncany/Testfolder/rootfolder");
			machineName = "WindowsMachine";
			
			home1 = new File("D:/Syncany/Testfolder/");
			home2 = new File("D:/Syncany/Testfolder2/");
			
			// "remote" folders
			appDir2 = new File("D:/Syncany/Testfolder2/applicationData");
			appCacheDir2 = new File("D:/Syncany/Testfolder2/cache");
			rootFolder2 = new File("D:/Syncany/Testfolder2/rootfolder");
		}
		else if (System.getProperty("os.name").contains("Linux")) {
			appDir = new File("/tmp/syncanytest/applicationData");
			appCacheDir = new File("/tmp/syncanytest/cache");
			repDir = new File("/tmp/syncanytest/repfolder");
			rootFolder = new File("/tmp/syncanytest/testfolder");
			machineName = "LinuxMachine";
			
			// "remote" folders
			appDir2 = new File("/tmp/syncanytest2/applicationData");
			appCacheDir2 = new File("/tmp/syncanytest2/cache");
			rootFolder2 = new File("/tmp/syncanytest2/testfolder");
		}	
		
		
		if(appDir.isDirectory() && !appDir.exists()) appDir.mkdirs();
		if(appCacheDir.isDirectory() && !appCacheDir.exists()) appCacheDir.mkdirs();
		if(repDir.isDirectory() && !repDir.exists()) repDir.mkdirs();
		if(rootFolder.isDirectory() && !appDir.exists()) appDir.mkdirs();
		if(appDir.isDirectory() && !appDir.exists()) appDir.mkdirs();
		
	}
	
	
	/** Creates an instance of the original application settings with the test settings attributes */
	public void createSettingsInstance() {
		// SETTING EVERYTHING IN SETTINGS-CLASS
		settings = Config.createInstance(appDir, appCacheDir, machineName);
	}
	
	public Profile createProfile() {
		Profile profile = Profile.getInstance();
		profile.getRepository().setChunkSize(chunkSize);
		profile.setRoot(rootFolder);
		return profile;
	}
	
	public boolean createCommunicationControlerInstance(Profile profile) {
		
    	profile.getEncryption().setPassword("password");
    	try {
			profile.getRepository().getEncryption().setSalt("SALT");
		} catch (EncryptionException e) {
			e.printStackTrace();
		} 
		
    	Map<String, String> connectionSettings = new LinkedHashMap<String, String>();
    	
    	
    	if(repDir!=null) connectionSettings.put("path", repDir.getAbsolutePath());
    	
    	
    	Connection conn = new LocalConnection();
    	conn.init(connectionSettings);
    	
    	if(profile.getRepository()!=null) {
    		profile.getRepository().setConnection(conn);
    		return true;
    	} else return false;
	}
	
	/** cleans appDir, cacheDir, repository and rootfolder */
	public void cleanAllFolders() {
		if(appDir!=null) FileTestHelper.emptyDirectory(appDir);
		if(appCacheDir!=null) FileTestHelper.emptyDirectory(appCacheDir);
		if(repDir!=null) FileTestHelper.emptyDirectory(repDir);
		if(rootFolder!=null) FileTestHelper.emptyDirectory(rootFolder);
	}
	
	/** Singleton */
	public static TestSettings getInstance() {
		if(instance==null) instance = new TestSettings();
		return instance;
	}

	public File getAppDir() {
		return appDir;
	}

	public File getAppCacheDir() {
		return appCacheDir;
	}

	public File getRepDir() {
		return repDir;
	}

	public File getRootFolder() {
		return rootFolder;
	}

	public String getMachineName() {
		return machineName;
	}

	public Config getSettings() {
		if(settings==null) createSettingsInstance();
		
		return settings;
	}

	public File getAppDir2() {
		return appDir2;
	}

	public void setAppDir2(File appDir2) {
		this.appDir2 = appDir2;
	}

	public File getAppCacheDir2() {
		return appCacheDir2;
	}

	public void setAppCacheDir2(File appCacheDir2) {
		this.appCacheDir2 = appCacheDir2;
	}

	public File getRootFolder2() {
		return rootFolder2;
	}

	public void setRootFolder2(File rootFolder2) {
		this.rootFolder2 = rootFolder2;
	}

	public String getConfigPath() {
		return configPath;
	}

	public void setConfigPath(String configPath) {
		this.configPath = configPath;
	}

	public String getLoggingPath() {
		return loggingPath;
	}

	public void setLoggingPath(String loggingPath) {
		this.loggingPath = loggingPath;
	}

	public String getConfigPath2() {
		return configPath2;
	}

	public void setConfigPath2(String configPath2) {
		this.configPath2 = configPath2;
	}

	public File getHome1() {
		return home1;
	}

	public void setHome1(File home1) {
		this.home1 = home1;
	}

	public File getHome2() {
		return home2;
	}

	public void setHome2(File home2) {
		this.home2 = home2;
	}

}
