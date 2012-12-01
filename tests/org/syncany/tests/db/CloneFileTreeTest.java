package org.syncany.tests.db;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.syncany.db.CloneFile;
import org.syncany.db.CloneFile.Status;
import org.syncany.db.CloneFileTree;
import org.syncany.db.DatabaseIO;
import org.syncany.tests.TestSettings;
import org.syncany.util.exceptions.CloneTreeException;

public class CloneFileTreeTest {
	private static CloneFileTree tree;
	
	
	private static File rootfolder;
	
	@BeforeClass
	public static void init(){
//		System.setProperty("syncany.home", "/opt/syncanytest");
		
		TestSettings testSettings = TestSettings.getInstance();
		testSettings.createSettingsInstance();
		
		testSettings.cleanAllFolders();
		
		rootfolder = testSettings.getRootFolder();
		
		tree = new CloneFileTree();
		
		CloneFile c1 = new CloneFile();
		c1.setPath(rootfolder.getAbsolutePath());
		c1.setName("a.txt");
		c1.setVersion(1);
		c1.setStatus(Status.NEW);
		// pseudo checksum
		c1.setChecksum(new byte[] {0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01});
		
		CloneFile c2 = new CloneFile();
		c2.setPath("/Users");
		c2.setName("ABC");
		c2.setVersion(1);
		c2.setStatus(Status.NEW);
		// pseudo checksum
		c2.setChecksum(new byte[] {0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x02});
		
		CloneFile c3 = new CloneFile();
		c3.setPath("/Users/D");
		c3.setName("c.txt");
		c3.setVersion(1);
		c3.setStatus(Status.NEW);
		// pseudo checksum
		c3.setChecksum(new byte[] {0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x03});
		
		try {
			tree.addCloneFile(c1);
			tree.addCloneFile(c2);
			tree.addCloneFile(c3);
			
		} catch (CloneTreeException e) {
			fail("CloneTreeException: " + e.getMessage());
		}
	}

	@Test()
	public void simpleWriteTest() {
		System.out.print("Saving to file");
		DatabaseIO.writeCompleteCloneFileTree(tree);
		System.out.println("\tOK");
	}
	
	@Test()
	public void simpleReadTest() {
		System.out.print("Reading from file");
		CloneFileTree t = DatabaseIO.readCompleteCloneFileTree();
		System.out.println("\tOK");
		System.out.println(t);
	}
}
