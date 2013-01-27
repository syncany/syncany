package org.syncany.tests.watcher.remote;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.syncany.communication.CommunicationController;
import org.syncany.config.Profile;
import org.syncany.config.Settings;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.db.CloneFile;
import org.syncany.db.Database;
import org.syncany.exceptions.InconsistentFileSystemException;
import org.syncany.exceptions.StorageException;
import org.syncany.tests.FileTestHelper;
import org.syncany.tests.TestSettings;
import org.syncany.watch.remote.ChangeManager;
import org.syncany.watch.remote.UpdateQueue;
import org.syncany.watch.remote.files.RemoteFile;
import org.syncany.watch.remote.files.UpdateFile;

public class ChangeManagerTest {

	private static final boolean DEBUG_GZIP_AND_ENCRYPT_UPDATE_FILES = false;
	
	private static boolean debug = true;
	
	private static ChangeManager changeManager;
	private static UpdateQueue updateList;
	private static Profile profile;
	private static Database db;
	private static UpdateFile localUpdateFile;
	
	private static Map<String, RemoteFile> remoteFileList;
	private static TransferManager transfer;
	
	@BeforeClass
	public static void init() {
		System.out.println("Initializing..\n");
		
		TestSettings testSettings = TestSettings.getInstance();
		testSettings.createSettingsInstance();
		
		if(debug) System.out.println("Cleaning directories..");
		FileTestHelper.emptyDirectory(testSettings.getAppCacheDir());
		FileTestHelper.emptyDirectory(testSettings.getAppDir());

		try {
			profile = Profile.getInstance();
			if(!testSettings.createCommunicationControlerInstance(profile)) throw new Exception("Couldn't configure Connection");
			
			transfer = profile.getRepository().getConnection().createTransferManager();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
		db = Database.getInstance();
		
		changeManager = new ChangeManager(profile);
		
		updateList = new UpdateQueue();
		
		try {
			localUpdateFile = new UpdateFile(profile.getRepository(), testSettings.getMachineName(), new Date());

			// Make temp. update file
			File tempLocalUpdateFile = profile.getCache().createTempFile("update-" + Settings.getInstance().getMachineName());
			System.out.println("Writing local changes to '" + tempLocalUpdateFile + "' ...");

			// changed due to profile deletion
			List<CloneFile> updatedFiles = db.getAddedCloneFiles();
			localUpdateFile.setVersions(updatedFiles);
			localUpdateFile.write(tempLocalUpdateFile, DEBUG_GZIP_AND_ENCRYPT_UPDATE_FILES);
			localUpdateFile.read(tempLocalUpdateFile, DEBUG_GZIP_AND_ENCRYPT_UPDATE_FILES);

//			tempLocalUpdateFile.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		updateList.setLocalUpdateFile(localUpdateFile);
		
		try {
			remoteFileList = transfer.list();
		} catch (StorageException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void changeManagerTest() {
		System.out.println("Starting ChangeManagerTest..\n");
		
		try {
			// TODO: because of no updates (of course) nothing happens..
			// Scenario Test 1 and 2 covers the functionality of the changemanager 
			changeManager.processUpdates(updateList);
		} catch (InconsistentFileSystemException e) {
			e.printStackTrace();
		}
		
	}
	
}
