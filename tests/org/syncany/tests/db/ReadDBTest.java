package org.syncany.tests.db;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.syncany.config.Settings;
import org.syncany.db.ChunkCache;
import org.syncany.db.CloneFileTree;
import org.syncany.db.DatabaseIO;

public class ReadDBTest {

	// Debug-Flag
	private boolean debug = false;
	
	@Test()
	public void simpleReadTest() {
		if(debug) System.out.print("Reading from file");

		Settings.getInstance().setAppDir(new File("/opt/syncanytest/db"));

		
		CloneFileTree t = DatabaseIO.readCompleteCloneFileTree();
		if(debug) System.out.println("\tOK");
		if(debug) System.out.println(t);
		
		Assert.assertNotNull("Read complete clone file tree from file failed -> CloneFileTree is null!", t);
	}
	
	@Test()
	public void simpleReadTest2() {
		if(debug) System.out.print("Reading from file");
		
		Settings.getInstance().setAppDir(new File("/opt/syncanytest2/db"));

		
		CloneFileTree t = DatabaseIO.readCompleteCloneFileTree();
		
		Assert.assertNotNull("Read complete clone file tree from file failed -> CloneFileTree is null!", t);

		if(debug) System.out.println("\tOK");
		if(debug) System.out.println(t);
	}
	
	@Test()
	public void simpleReadTestChunks() {
		if(debug) System.out.print("Reading from file");
		
		Settings.getInstance().setAppDir(new File("/opt/syncanytest/db"));
		
		ChunkCache cm = new ChunkCache();

		Assert.assertNotNull("Creating ChunkCache failed -> ChunkCache is null!", cm);
		
		if(debug) System.out.println("\tOK");
		if(debug) System.out.println(cm);
	}
}
