package org.syncany.tests;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.syncany.Constants;
import org.syncany.config.ConfigTO;
import org.syncany.config.EncryptionException;
import org.syncany.config.Profile;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.local.LocalConnection;


public class TestEnvironment {
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
	
	private TestEnvironment() {
		File systemSpecificTestDir = new File(System.getProperty("java.io.tmpdir")+"/syncanytest");
		
		appDir = new File(systemSpecificTestDir+"/applicationData");
		appCacheDir = new File(systemSpecificTestDir+"/cache");
		repDir = new File(systemSpecificTestDir+"/repfolder");
		rootFolder = new File(systemSpecificTestDir+"/testfolder");
		machineName = "MACMachine";
		
		home1 = new File(systemSpecificTestDir+"/home1");
		home2 = new File(systemSpecificTestDir+"/home2");
		
		// "remote" folders
		appDir2 = new File(systemSpecificTestDir+"/applicationData");
		appCacheDir2 = new File("/opt/syncanytest2/cache");
		rootFolder2 = new File("/opt/syncanytest2/testfolder");	  
		
		if(appDir.isDirectory() && !appDir.exists()) appDir.mkdirs();
		if(appCacheDir.isDirectory() && !appCacheDir.exists()) appCacheDir.mkdirs();
		if(repDir.isDirectory() && !repDir.exists()) repDir.mkdirs();
		if(rootFolder.isDirectory() && !appDir.exists()) appDir.mkdirs();
		if(appDir.isDirectory() && !appDir.exists()) appDir.mkdirs();
		
	}
	
	
	public Profile createProfile() {
		Profile profile = new Profile();
		profile.setChunkSize(chunkSize);
		profile.setRoot(rootFolder);
		return profile;
	}	
	
	/** cleans appDir, cacheDir, repository and rootfolder */
	public void cleanAllFolders() {
		if(appDir!=null) TestUtil.emptyDirectory(appDir);
		if(appCacheDir!=null) TestUtil.emptyDirectory(appCacheDir);
		if(repDir!=null) TestUtil.emptyDirectory(repDir);
		if(rootFolder!=null) TestUtil.emptyDirectory(rootFolder);
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
