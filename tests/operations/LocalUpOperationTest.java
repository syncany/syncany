package operations;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.config.ConfigTO;
import org.syncany.config.Profile;
import org.syncany.operations.SyncDownOperation;
import org.syncany.tests.util.TestUtil;

public class LocalUpOperationTest {

	private File tempLocalDir;
	private File tempRepoDir;
	
	@Before
	public void setUp() throws Exception {
		tempLocalDir = TestUtil.createTempDirectoryInSystemTemp();
		tempRepoDir = TestUtil.createTempDirectoryInSystemTemp();
	}
	
	@After
	public void tearDown() throws Exception {
		TestUtil.deleteDirectory(tempLocalDir);
		TestUtil.deleteDirectory(tempRepoDir);
	}

	@Test
	public void testUploadLocalDatabase() throws Exception {
		int fileSize = 1230;
		int fileAmount = 2;
		//Fill local repo with random files
		List<File> originalFiles = TestUtil.generateRandomBinaryFilesInDirectory(tempLocalDir, fileSize,
				fileAmount);
		
		//CreateTest Profile
		ConfigTO configTO = new ConfigTO() {
			
		};
		
		Profile profile = new Profile(configTO);
		
		SyncDownOperation op = new SyncDownOperation(profile);
		op.execute();
		
		//Compare dbs
		
		//compare files listed in db remote & local 
	}
	
}
