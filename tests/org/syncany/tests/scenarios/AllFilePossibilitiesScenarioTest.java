package org.syncany.tests.scenarios;

import static org.syncany.tests.util.TestAssertUtil.assertDatabaseFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.scenarios.framework.AbstractClientAction;
import org.syncany.tests.scenarios.framework.ChangeContentWithoutFileSize;
import org.syncany.tests.scenarios.framework.ChangePermissionsOfFile;
import org.syncany.tests.scenarios.framework.ChangeFileSize;
import org.syncany.tests.scenarios.framework.ChangeLastModifiedDate;
import org.syncany.tests.scenarios.framework.ChangePermissionsOfFolder;
import org.syncany.tests.scenarios.framework.ChangeSymlinkTarget;
import org.syncany.tests.scenarios.framework.ChangeTypeFolderToFile;
import org.syncany.tests.scenarios.framework.ChangeTypeFileToFolder;
import org.syncany.tests.scenarios.framework.ChangeTypeFileToSymlinkWithTargetFile;
import org.syncany.tests.scenarios.framework.ChangeTypeFileToSymlinkWithTargetFolder;
import org.syncany.tests.scenarios.framework.ChangeTypeFileToSymlinkWithNonExistingTarget;
import org.syncany.tests.scenarios.framework.ChangeTypeSymlinkWithTargetFileToFolder;
import org.syncany.tests.scenarios.framework.ChangeTypeSymlinkWithTargetFolderToFolder;
import org.syncany.tests.scenarios.framework.ClientActions;
import org.syncany.tests.scenarios.framework.CreateFile;
import org.syncany.tests.scenarios.framework.CreateFileTree;
import org.syncany.tests.scenarios.framework.CreateFolder;
import org.syncany.tests.scenarios.framework.CreateSymlinkToFile;
import org.syncany.tests.scenarios.framework.CreateSymlinkToFolder;
import org.syncany.tests.scenarios.framework.CreateSymlinkToNonExisting;
import org.syncany.tests.scenarios.framework.DeleteFile;
import org.syncany.tests.scenarios.framework.DeleteFolder;
import org.syncany.tests.scenarios.framework.Executable;
import org.syncany.tests.scenarios.framework.LockFile;
import org.syncany.tests.scenarios.framework.MoveFileToOtherFolder;
import org.syncany.tests.scenarios.framework.MoveFileWithinFolder;
import org.syncany.tests.scenarios.framework.MoveFolderToOtherFolder;
import org.syncany.tests.scenarios.framework.MoveFolderWithinFolder;
import org.syncany.tests.scenarios.framework.UnlockFile;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
/**
 * attributes:
 * x- size
 * x- type
 * x- content (without size-change)
 * x- name
 * x- path
 * - permissions (linux / windows)
 * x- last modified date
 * 
 * xcreate file
 * xmove file
 * xchange file without changing size
 * xchange file with changing size
 * xchange file type - folder to file
 * xchange file type - file to folder
 * (change file type - folder to symlink)
 * x(change file type - file to symlink)
 * xdelete file
 * 
 * xcreate folder
 * xmove folder
 * xdelete folder
 * 
 * xmove file to subfolder
 * xmove folder to subfolder
 * 
 * (create symlink folder)
 * (change symlink folder target)
 * (delete symlink folder)
 * 
 * file vanishes during index process
 * folder vanishes during index process
 * 
 * file is changed during sync down operation 
 * file is changed during sync up operation
 * 
 * file permission denied
 */		
public class AllFilePossibilitiesScenarioTest {	
	@Test
	public void testAllPossibilities() throws Exception {		
		final Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		final TestClient clientA = new TestClient("A", testConnection);
		final TestClient clientB = new TestClient("B", testConnection);
		
		ClientActions.runOps(clientA,
			new Executable() {
				@Override
				public void execute() throws Exception {
					// Nothing.
				}			
			},
			new AbstractClientAction[] {
				new CreateFileTree(),
				
				new ChangeContentWithoutFileSize(),
				new ChangeFileSize(), 
				new ChangeLastModifiedDate(),
				new ChangePermissionsOfFile(),
				new ChangePermissionsOfFolder(), 
				new ChangeSymlinkTarget(),				
				new ChangeTypeFileToFolder(),
				new ChangeTypeFileToSymlinkWithNonExistingTarget(),
				new ChangeTypeFileToSymlinkWithTargetFile(),
				new ChangeTypeFileToSymlinkWithTargetFolder(),
				new ChangeTypeSymlinkWithTargetFileToFolder(),
				new ChangeTypeSymlinkWithTargetFolderToFolder(),
				new ChangeTypeFolderToFile(), // TODO [medium] Implement rest of change type tests
				new CreateFile(),
				new CreateFolder(),
				new CreateSymlinkToFile(),
				new CreateSymlinkToFolder(),
				new CreateSymlinkToNonExisting(),
				new DeleteFile(),
				new DeleteFolder(),
				new LockFile(),
				new MoveFileToOtherFolder(),
				new MoveFileWithinFolder(),
				new MoveFolderToOtherFolder(),
				new MoveFolderWithinFolder(),
				new UnlockFile() // Must be after LockFile
			},
			new Executable() {
				@Override
				public void execute() throws Exception {
					clientA.upWithForceChecksum();		
					
					clientB.down();
					assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
					assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());					
				}			
			}
		);
		
		clientA.cleanup();
		clientB.cleanup();
	}
	
}
