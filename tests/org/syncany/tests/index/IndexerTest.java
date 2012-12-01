package org.syncany.tests.index;


import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.syncany.config.Profile;
import org.syncany.db.CloneFile;
import org.syncany.db.CloneFile.Status;
import org.syncany.db.Database;
import org.syncany.index.Indexer;
import org.syncany.index.requests.DeleteIndexRequest;
import org.syncany.index.requests.MoveIndexRequest;
import org.syncany.index.requests.NewIndexRequest;
import org.syncany.tests.FileTestHelper;
import org.syncany.tests.TestSettings;

/** 
 * IndexerTest tests all IndexRequests.
 * <p>
 * The conducted tests are the following:
 * <ul>
 * <li>1. Indexing a new file (direct in root)</li>
 * <li>2. Indexing new folder (direct in root) with a file inside</li>
 * <li>3. File rename of file in rootfolder</li>
 * <li>4. Folder rename (with the file inside)</li>
 * <li>5. Moving file from subfolder to root</li>
 * <li>6. Deleting renamed file of step 3</li>
 * <li>7. Deleting renamed folder of step 4</li>
 * </ul>
 */
public class IndexerTest {
	
	// Debug-Flag
	private static boolean debug = false;

	private static Indexer indexer;
	private static Database db;
	private static Profile profile;
	private static File rootFolder;
	
	private static TestSettings testSettings;
	
	
	private static final int fileSize = 1024*1024; // 100 KB
	private static File randomlyCreatedFile, moveFile, toFileRename, toFileMove, folder, toFolder;
    private static String fileName1, fileName2, moveFileName, folderName = "myFolder", folderName2 = "myFolder2";
	
    
    private static int waitingTime = 5000;
    
    private static boolean newIndexRequestProcessedSuccessfullyForFile = false;
    private static boolean newIndexRequestProcessedSuccessfullyForFolder = false;
	
    /** Preparing files and folders for the tests */
	@BeforeClass
	public static void init() {
		testSettings = TestSettings.getInstance();
		testSettings.createSettingsInstance();
		testSettings.cleanAllFolders();
		
		rootFolder = testSettings.getRootFolder();
		
		profile = testSettings.createProfile();
		
		indexer = Indexer.getInstance();
		db = Database.getInstance();
		
		db.clearNewAddedFiles();
		db.cleanChunkCache();
		
		try {
			String rootPath = rootFolder.getAbsolutePath()+File.separator;
			String filePath;
			
			// random file names in order not to overwrite a possibly existing old file..
			fileName1 = ("testfile-"+Math.random()+"-one").replace(".", ""); // first file - origin filename
			fileName2 = ("testfile-"+Math.random()+"-two").replace(".", "");  // first file - new filename for renaming
			moveFileName = ("testfile-"+Math.random()+"-move").replace(".", "");  // second file for move operation
			
			// creating file in root folder
			filePath = rootPath+fileName1;
			FileTestHelper.generateRandomBinaryFile(new File(filePath), fileSize);
			randomlyCreatedFile = new File(filePath);
			if(debug) System.out.println("created file \""+fileName1+"\" at Location \""+rootPath+"\"");
			
			// creating folder in root folder
			String folderPath = rootPath+folderName;
			folder = new File(folderPath);
			folder.mkdirs();
			if(debug) System.out.println("created folder \""+folderName+"\" at Location \""+rootPath+"\"");
			
			// creating a further file in the previous generated folder
			filePath = rootPath+folderName+File.separator+moveFileName;
			FileTestHelper.generateRandomBinaryFile(new File(filePath), fileSize);
			moveFile = new File(filePath);
			if(debug) System.out.println("created file \""+moveFileName+"\" at Location \""+rootPath+folderName+File.separator+"\"");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/** NewIndexRequest for a File (triggered directly)
	 * 
	 * @throws InterruptedException timer exception
	 */
	@Test()
	public void testNewIndexRequestForFile() throws InterruptedException {
		if(debug) {
			System.out.println("\n----------------------------");
			System.out.println("-> Testing NewIndexRequest for File..\n");
		}
		
		NewIndexRequest newIndexRequest = new NewIndexRequest(randomlyCreatedFile, null); // null = no previous version of this file
		
		newIndexRequest.process();
		
		// Wait
		Thread.sleep(waitingTime);
		
		if(testSettings.getAppCacheDir()!=null) {
			File[] files = testSettings.getAppCacheDir().listFiles();
			
			Assert.assertTrue("no metachunks generated of file "+fileName1, files!=null && files.length>0);
			
			for(CloneFile cf : db.getAddedCloneFiles()) {
				
				if(debug) print(cf, false);
				
				if(debug) System.out.print("Validating NewIndexRequest for File \""+fileName1+"\".. ");
				
				Assert.assertTrue("Generated File \""+fileName1+"\" is not of type file!", !cf.isFolder());
				Assert.assertTrue("Generated File \""+fileName1+"\" has not Status \"NEW\"!", cf.getStatus().equals(Status.NEW));
				Assert.assertTrue("Generated File \""+fileName1+"\" and last added clone file have different names!", cf.getName()!=null && cf.getName().equals(randomlyCreatedFile.getName()));
				Assert.assertTrue("Generated File \""+fileName1+"\" has not version 1", cf.getVersion()==1);
				Assert.assertTrue("Version History of generated file \""+fileName1+"\" is not 1", cf.getVersionHistory()!=null && cf.getVersionHistory().size()==1);
				Assert.assertTrue("Path of generated file \""+fileName1+"\" is not empty and hence not lying in the root", cf.getPath()==null || cf.getPath().equals(""));
				
				if(debug) System.out.println("OK!\n");
			}
			
			db.clearNewAddedFiles();
			
		} else {
			Assert.fail("AppCacheDir is null!");
		}
		
		// cleaning cache and db for next test
		FileTestHelper.emptyDirectory(testSettings.getAppCacheDir());
		FileTestHelper.emptyDirectory(testSettings.getAppDir());
		db.cleanChunkCache();
		
		
		newIndexRequestProcessedSuccessfullyForFile = true;
	}
	
	/** NewIndexRequest for a Folder (triggered directly)
	 * 
	 * @throws InterruptedException timer exception
	 */
	@Test()
	public void testNewIndexRequestForFolder() throws InterruptedException {
		if(debug) {
			System.out.println("\n----------------------------");
			System.out.println("-> Testing NewIndexRequest for Folder..\n");
		}
		
		NewIndexRequest newIndexRequest = new NewIndexRequest(folder, null); // null = no previous version of this file
		
		newIndexRequest.process();
		
		// Wait
		Thread.sleep(waitingTime);
		
		
		for(CloneFile cf : db.getAddedCloneFiles()) {
			
			if(debug) print(cf, false);
			
			Assert.assertTrue("CloneFile has no Name!", cf.getName()!=null);
			
			if(cf.getName().equals(folderName)) {
				if(debug) System.out.print("Validating NewIndexRequest for Folder \""+folderName+"\".. ");
				
				Assert.assertTrue("Generated folder \""+folderName+"\" has not Status \"NEW\"!", cf.getStatus().equals(Status.NEW));
				Assert.assertTrue("Generated folder \""+folderName+"\" is not of type folder!", cf.isFolder());
				Assert.assertTrue("Generated folder \""+folderName+"\" and last added clone file \""+cf.getName()+"\" have different names!", cf.getName().equals(folder.getName()));
				Assert.assertTrue("Generated folder \""+folderName+"\" has not version 1", cf.getVersion()==1);
				Assert.assertTrue("Version History of generated folder \""+folderName+"\" is not 1", cf.getVersionHistory()!=null && cf.getVersionHistory().size()==1);
				Assert.assertTrue("Path of generated folder \""+folderName+"\" is not empty and hence not lying in the root", cf.getPath()==null || cf.getPath().equals(""));
				
				if(debug) System.out.println("OK!\n");
			} else if(cf.getName().equals(moveFileName)) {
				if(debug) System.out.print("Validating NewIndexRequest for File \""+moveFileName+"\".. ");
				
				Assert.assertTrue("Generated File \""+moveFileName+"\" is not of type file!", !cf.isFolder());
				Assert.assertTrue("Generated File \""+moveFileName+"\" has not Status \"NEW\"!", cf.getStatus().equals(Status.NEW));
				Assert.assertTrue("Generated File \""+moveFileName+"\" and last added clone file have different names!", cf.getName().equals(moveFile.getName()));
				Assert.assertTrue("Generated File \""+moveFileName+"\" has not version 1", cf.getVersion()==1);
				Assert.assertTrue("Version History of generated file \""+moveFileName+"\" is not 1", cf.getVersionHistory()!=null && cf.getVersionHistory().size()==1);
				Assert.assertTrue("Path of generated File \""+moveFileName+"\" is not the name of the subfolder \""+folderName+"\"", cf.getPath()!=null && cf.getPath().equals(folderName));
				
				if(debug) System.out.println("OK!\n");
				
			} else Assert.fail("Unknown CloneFile Name: "+cf.getName());
		}
		
		db.clearNewAddedFiles();
		
		// cleaning cache and db for next test
		FileTestHelper.emptyDirectory(testSettings.getAppCacheDir());
		FileTestHelper.emptyDirectory(testSettings.getAppDir());
		db.cleanChunkCache();
		
		
		newIndexRequestProcessedSuccessfullyForFolder = true;
	}
	
	/** Testing the MoveIndexRequest (file rename operation)
	 * @throws InterruptedException timer exception
	 */
	@Test()
	public void testMoveIndexRequestFileRename() throws InterruptedException {
		Assert.assertTrue("testNewIndexRequest has to be conducted first", newIndexRequestProcessedSuccessfullyForFile);
		
		if(debug) {
			System.out.println("\n-----------------------------");
			System.out.println("-> Testing MoveIndexRequest (FileRename) ..\n");
		}
		
		db.clearNewAddedFiles();
		
		toFileRename = new File(rootFolder.getAbsolutePath()+File.separator+fileName2);
		randomlyCreatedFile.renameTo(toFileRename);
		
		MoveIndexRequest moveIndexRequest = new MoveIndexRequest(randomlyCreatedFile, toFileRename);
		
		moveIndexRequest.process();
		
		// Wait
		Thread.sleep(waitingTime);
		
		long highestFileVersion = 0;
		
		for(CloneFile cf : db.getAddedCloneFiles()) {

			if(debug) print(cf, false);
			
			// Updating highest version count
			if(cf.getVersion()>highestFileVersion) highestFileVersion = cf.getVersion();

			
			Assert.assertTrue("CloneFile has no Name!", cf.getName()!=null);
			
			if(cf.getVersion()==1) {
				Assert.assertTrue("Generated File \""+fileName1+"\" and last added clone file \""+cf.getName()+"\" have different names!", fileName1.equals(cf.getName()));
			}
			else if(cf.getVersion()==2) {
				CloneFile previousCF = cf.getPrevious();
				Assert.assertTrue("No previous version of the file \""+fileName2+"\" found.", previousCF!=null);
				Assert.assertTrue("Generated File \""+fileName2+"\" has not Status \"RENAMED\"!", cf.getStatus().equals(Status.RENAMED));
				Assert.assertTrue("Name of previous File Version \""+fileName1+"\" and clone file \""+previousCF.getName()+"\" have different names!", fileName1.equals(previousCF.getName()));
				Assert.assertTrue("Generated File \""+fileName2+"\" and last added clone file \""+cf.getName()+"\" have different names!", fileName2.equals(cf.getName()));
			}
			else Assert.fail("Unknown Version: "+cf.getVersion());
			
			Assert.assertTrue("Generated file \""+cf.getName()+"\" is not of type file!", !cf.isFolder());
			Assert.assertTrue("Path of generated file \""+cf.getName()+"\" is not empty and hence not lying in the root", cf.getPath()==null || cf.getPath().equals(""));
		}
		
		Assert.assertTrue("The highest file version was not 2!", highestFileVersion==2);
		
		db.clearNewAddedFiles();
	}
	
	/** Testing the MoveIndexRequest (folder rename operation)
	 * @throws InterruptedException timer exception
	 */
	@Test()
	public void testMoveIndexRequestFolderRename() throws InterruptedException {
		Assert.assertTrue("testNewIndexRequest for Folder has to be conducted first", newIndexRequestProcessedSuccessfullyForFolder);
		
		if(debug) {
			System.out.println("\n-----------------------------");
			System.out.println("-> Testing MoveIndexRequest (FolderRename) ..\n");
		}
		
		db.clearNewAddedFiles();
		
		toFolder = new File(rootFolder.getAbsolutePath()+File.separator+folderName2);
		folder.renameTo(toFolder);
		
		MoveIndexRequest moveIndexRequest = new MoveIndexRequest(folder, toFolder);
		
		moveIndexRequest.process();
		
		// Wait
		Thread.sleep(waitingTime);
		
		for(CloneFile cf : db.getAddedCloneFiles()) {

			if(debug) print(cf, false);
			
			Assert.assertTrue("CloneFile has no Name!", cf.getName()!=null);
			
			if(folderName2.equals(cf.getName())) {
				CloneFile previousCF = cf.getPrevious();
				Assert.assertTrue("No previous version of the folder \""+folderName2+"\" found.", previousCF!=null);
				Assert.assertTrue("Version of renamed folder \""+folderName2+"\" is not 2!", cf.getVersion()==2);
				Assert.assertTrue("Renamed folder \""+folderName2+"\" has not Status \"RENAMED\"!", cf.getStatus().equals(Status.RENAMED));
				Assert.assertTrue("Name of previous folder Version \""+folderName+"\" and clone file \""+previousCF.getName()+"\" have different names!", folderName.equals(previousCF.getName()));
				Assert.assertTrue("Renamed Folder \""+folderName2+"\" and last added clone file \""+cf.getName()+"\" have different names!", folderName2.equals(cf.getName()));
				Assert.assertTrue("Renamed folder \""+cf.getName()+"\" is not of type folder!", cf.isFolder());
			} else if(folderName.equals(cf.getName())) {
				Assert.assertTrue("Version of origin folder \""+folderName+"\" is not 1!", cf.getVersion()==1);
				Assert.assertTrue("Folder \""+folderName+"\" has not Status \"NEW\"!", cf.getStatus().equals(Status.NEW));
				Assert.assertTrue("Created folder \""+cf.getName()+"\" is not of type folder!", cf.isFolder());
			} else if(moveFileName.equals(cf.getName())) {
				if(cf.getVersion()==1) {
					Assert.assertTrue("Version of File \""+moveFileName+"\" in origin folder \""+folderName+"\" is not 1!", cf.getVersion()==1);
					Assert.assertTrue("File of folder \""+moveFileName+"\" has not Status \"NEW\"!", cf.getStatus().equals(Status.NEW));
					Assert.assertTrue("Path of File \""+moveFileName+"\" in the origin Folder \""+folderName+"\" is not the name of the clone file folder", cf.getPath()!=null && cf.getPath().equals(folderName));
				} else if(cf.getVersion()==2) {
					Assert.assertTrue("Version of File \""+moveFileName+"\" in renamed folder \""+folderName2+"\" is not 2!", cf.getVersion()==2);
					Assert.assertTrue("File of folder \""+moveFileName+"\" has not Status \"RENAMED\"!", cf.getStatus().equals(Status.RENAMED));
					Assert.assertTrue("Path of File \""+moveFileName+"\" in the renamed Folder \""+folderName2+"\" is not the name of the renamed folder \""+folderName2+"\"", cf.getPath()!=null && cf.getPath().equals(folderName2));
				}
				
				Assert.assertTrue("Moved File \""+cf.getName()+"\" is not of type file!", !cf.isFolder());
			}
			else Assert.fail("Unknown Version: "+cf.getVersion());
		}
		
		moveFile = new File(rootFolder.getAbsolutePath()+File.separator+folderName2+File.separator+moveFileName);
		
		db.clearNewAddedFiles();
	}
	
	/** Testing the MoveIndexRequest (move operation of a file from Folder A to folder B)
	 * 
	 * @throws InterruptedException timer exception
	 */
	@Test()
	public void testMoveIndexRequestFileMove() throws InterruptedException {
		Assert.assertTrue("testNewIndexRequest has to be conducted first", newIndexRequestProcessedSuccessfullyForFolder);
		
		if(debug) {
			System.out.println("\n-----------------------------");
			System.out.println("-> Testing MoveIndexRequest (FileMove) ..\n");
		}
		
		db.clearNewAddedFiles();
		
		if(debug) System.out.println("moving \""+moveFileName+"\" to "+(rootFolder.getAbsolutePath()+File.separator+moveFileName));
		toFileMove = new File(rootFolder.getAbsolutePath()+File.separator+moveFileName);
		moveFile.renameTo(toFileMove);
		
		MoveIndexRequest moveIndexRequest = new MoveIndexRequest(moveFile, toFileMove);
		
		moveIndexRequest.process();
		
		// Wait
		Thread.sleep(waitingTime);

		
		for(CloneFile cf : db.getAddedCloneFiles()) {

			if(debug) print(cf, false);
			
			if(cf.getVersion()==1) {
				if(debug) System.out.print("Validating MoveIndexRequest for Move Operation for File \""+moveFileName+"\" for Version 1.. ");
				
				Assert.assertTrue("Previous file version is not of type file!", !cf.isFolder());
				Assert.assertTrue("Generated File \""+moveFileName+"\" has not Status \"NEW\"!", cf.getStatus().equals(Status.NEW));
				Assert.assertTrue("Path of the previous version of the file \""+moveFileName+"\" is not the name of the subfolder \""+folderName+"\"", cf.getPath()!=null && cf.getPath().equals(folderName));
				Assert.assertTrue("Name of 1st (previous) version of the CloneFile is equal to \""+moveFileName+"\"", moveFileName.equals(cf.getName()));
				
				if(debug) System.out.println("OK!\n");
			} else if(cf.getVersion()==2) {
				if(debug) System.out.print("Validating MoveIndexRequest for Move Operation for File \""+moveFileName+"\" for Version 2.. ");
				
				CloneFile previousCF = cf.getPrevious();
				Assert.assertTrue("No previous version of the file \""+moveFileName+"\" found.", previousCF!=null);
				Assert.assertTrue("Version 2 of File \""+moveFileName+"\" has not Status \"RENAMED\"!", cf.getStatus().equals(Status.RENAMED));
				Assert.assertTrue("Previous version of the file has not version 1", previousCF.getVersion()==1);
				Assert.assertTrue("Name of 2st (current) version of the CloneFile is equal to \""+moveFileName+"\"", moveFileName.equals(cf.getName()));
				Assert.assertTrue("Current file version is not of type file!", !cf.isFolder());
				Assert.assertTrue("Path of the previous version of the file \""+moveFileName+"\" is not the name of the subfolder \""+folderName2+"\"", cf.getPath()!=null && cf.getPath().equals(folderName2));
				
				if(debug) System.out.println("OK!\n");
			} else if(cf.getVersion()==3) {
			if(debug) System.out.print("Validating MoveIndexRequest for Move Operation for File \""+moveFileName+"\" for Version 3.. ");
				
				CloneFile previousCF = cf.getPrevious();
				Assert.assertTrue("No previous version of the file \""+moveFileName+"\" found.", previousCF!=null);
				Assert.assertTrue("Version 2 of File \""+moveFileName+"\" has not Status \"RENAMED\"!", cf.getStatus().equals(Status.RENAMED));
				Assert.assertTrue("Previous version of the file has not version 2", previousCF.getVersion()==2);
				Assert.assertTrue("Name of 2st (current) version of the CloneFile is equal to \""+moveFileName+"\"", moveFileName.equals(cf.getName()));
				Assert.assertTrue("Current file version is not of type file!", !cf.isFolder());
				Assert.assertTrue("Path of current version of the file \""+moveFileName+"\" is not empty and hence not lying in the root", cf.getPath()==null || cf.getPath().equals(""));
				
				if(debug) System.out.println("OK!\n");
			} else Assert.fail("Unknown Version: "+cf.getVersion());
		}
		
		db.clearNewAddedFiles();
	}
	
	
	
	/** Testing the DeleteIndexRequest for a File
	 * 
	 * @throws InterruptedException timer exception
	 */
	@Test()
	public void testDeleteIndexRequestForFile() throws InterruptedException {
		Assert.assertTrue("testNewIndexRequest for File has to be conducted first", newIndexRequestProcessedSuccessfullyForFile);
		
		if(debug) {
			System.out.println("\n-------------------------------");
			System.out.println("-> Testing DeleteIndexRequest for a File..\n");
		}
		
		// Deleting File
		Assert.assertTrue("Could not delete File \""+toFileRename.getAbsolutePath()+"\" for DeleteIndexRequest", !toFileRename.exists() || toFileRename.delete());
		
		DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(toFileRename);

		deleteIndexRequest.process();
		
		// Wait
		Thread.sleep(waitingTime);
		
		if(debug) System.out.println("Counting");
		int i = 0;
		for(CloneFile cf : db.getAddedCloneFiles()) {
			if(debug) System.out.println("i: "+i);
			i++;
			
			if(debug) print(cf, false);
			
			if(cf.getVersion()==3) {
				if(debug) System.out.print("Validating DeleteIndexRequest for File \""+fileName2+"\".. ");
				
				Assert.assertTrue("Deleted File \""+fileName2+"\" is not of type file!", !cf.isFolder());
				Assert.assertTrue("Deleted File \""+fileName2+"\" and last added clone file have different names!", cf.getName().equals(toFileRename.getName()));
				Assert.assertTrue("Deleted File \""+fileName2+"\" has no previous versions", cf.getPrevious()!=null);
				Assert.assertTrue("Deleted File \""+fileName2+"\" has not Status deleted in last version", cf.getStatus().equals(Status.DELETED));
				Assert.assertTrue("Deleted File \""+fileName2+"\" is not the last version of this File", cf.getNext()==null);
				Assert.assertTrue("Version History of deleted file \""+fileName2+"\" is not 3", cf.getVersionHistory()!=null && cf.getVersionHistory().size()==3);
				Assert.assertTrue("Path of deleted File \""+fileName2+"\" is not empty and hence not lying in the root", cf.getPath()==null || cf.getPath().equals(""));
				
				if(debug) System.out.println("OK!\n");
			}
		}
		
		Assert.assertTrue("Not iterated over 2 clone files", i==2);
		
		db.clearNewAddedFiles();
	}
	
	/** Testing the DeleteIndexRequest for a Folder
	 * 
	 * @throws InterruptedException timer exception
	 */
	@Test()
	public void testDeleteIndexRequestForFolder() throws InterruptedException {
		Assert.assertTrue("testNewIndexRequest for Folder has to be conducted first", newIndexRequestProcessedSuccessfullyForFolder);
		
		if(debug) {
			System.out.println("\n-------------------------------");
			System.out.println("-> Testing DeleteIndexRequest for a Folder..\n");
		}

		
		// Deleting File and Folder
		FileTestHelper.emptyDirectory(toFolder);
		Assert.assertTrue("Could not delete Folder \""+toFolder.getAbsolutePath()+"\" for DeleteIndexRequest", !toFolder.exists() || toFolder.delete());
		
		DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(toFolder);

		deleteIndexRequest.process();
		
		// Wait
		Thread.sleep(waitingTime);
		
		if(debug) System.out.println("Counting");
		int i = 0;
		for(CloneFile cf : db.getAddedCloneFiles()) {
			if(debug) System.out.println("i: "+i++);
			
			if(debug) print(cf, false);
			
			if(folderName2.equals(cf.getName())) {
				
				if(cf.getVersion()==3) {
					Assert.assertTrue("Deleted Folder \""+folderName2+"\" is not of type folder!", cf.isFolder());
					Assert.assertTrue("Deleted Folder \""+folderName2+"\" and current clone file have different names!", cf.getName().equals(toFolder.getName()));
					Assert.assertTrue("Deleted Folder \""+folderName2+"\" has no previous versions", cf.getPrevious()!=null);
					Assert.assertTrue("Deleted Folder \""+folderName2+"\" has not Status deleted in last version", cf.getStatus().equals(Status.DELETED));
					Assert.assertTrue("Deleted Folder \""+folderName2+"\" is not the last version of this Folder", cf.getNext()==null);
					Assert.assertTrue("Version History of deleted folder \""+folderName2+"\" is not 3", cf.getVersionHistory()!=null && cf.getVersionHistory().size()==3);
					Assert.assertTrue("Path of deleted Folder \""+folderName2+"\" is not empty and hence not lying in the root", cf.getPath()==null || cf.getPath().equals(""));
				} else if(cf.getVersion()==2) {
					Assert.assertTrue("Folder in Version 2: \""+folderName2+"\" is not of type folder!", cf.isFolder());
					Assert.assertTrue("Folder in Version 2: \""+folderName2+"\" has not Status \"Renamed\"!", cf.getStatus().equals(Status.RENAMED));
					Assert.assertTrue("Folder in Version 2: Path of Folder \""+folderName2+"\" is not empty and hence not lying in the root", cf.getPath()==null || cf.getPath().equals(""));
				}
			} 
			else Assert.fail("Unknown Name: "+cf.getName());
			
		}
		db.clearNewAddedFiles();
	}
	

	/** Screen printing method 
	 * @param cf CloneFile for the printing
	 * @param details Flag if details should be printed too
	 */
	private static void print(CloneFile cf, boolean details) {
		
		System.out.println("\n--- CloneFile-Output ---");
//		System.out.println("cf.toString: "+cf.toString());
		System.out.println("cf.print: "+cf.print());
		
		if(details) {
			System.out.println("Name: "+cf.getName());
			System.out.println("Has previous File? "+(cf.getPrevious()!=null));
			if(cf.getPrevious()!=null) System.out.println("Previous Versions Name: "+cf.getPrevious().getName());
			
			System.out.println("Has next File? "+(cf.getNext()!=null));
			if(cf.getNext()!=null) System.out.println("Next Versions Name: "+cf.getNext().getName());
			
			System.out.println("cf.getPath: "+cf.getPath());
			System.out.println("cf.getFullPath(): "+cf.getFullPath());
			
			System.out.println("cf.getVersion:"+cf.getVersion());
			
			// Java Heap Space exception
//			System.out.println("cf.getVersionHistory:"+cf.getVersionHistory());
//			System.out.println("cf.getVersionHistory().size(): "+cf.getVersionHistory().size());
		}
		System.out.println("---");		
	}
	
	@AfterClass
	public static void cleanup() throws Exception {
		// cleaning all folders
		testSettings.cleanAllFolders();
		db.clearNewAddedFiles();
		db.cleanChunkCache();
	}

}
