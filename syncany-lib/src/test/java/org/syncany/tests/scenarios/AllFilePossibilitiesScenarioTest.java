/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.tests.scenarios;

import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;
import static org.syncany.tests.util.TestAssertUtil.assertSqlDatabaseEquals;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.scenarios.framework.AbstractClientAction;
import org.syncany.tests.scenarios.framework.ChangeContentWithoutFileSize;
import org.syncany.tests.scenarios.framework.ChangeFileSize;
import org.syncany.tests.scenarios.framework.ChangeLastModifiedDate;
import org.syncany.tests.scenarios.framework.ChangePermissionsOfFile;
import org.syncany.tests.scenarios.framework.ChangePermissionsOfFolder;
import org.syncany.tests.scenarios.framework.ChangeSymlinkTarget;
import org.syncany.tests.scenarios.framework.ChangeTypeFileToFolder;
import org.syncany.tests.scenarios.framework.ChangeTypeFileToSymlinkWithNonExistingTarget;
import org.syncany.tests.scenarios.framework.ChangeTypeFileToSymlinkWithTargetFile;
import org.syncany.tests.scenarios.framework.ChangeTypeFileToSymlinkWithTargetFolder;
import org.syncany.tests.scenarios.framework.ChangeTypeFolderToFile;
import org.syncany.tests.scenarios.framework.ChangeTypeFolderToSymlinkWithNonExistingTarget;
import org.syncany.tests.scenarios.framework.ChangeTypeFolderToSymlinkWithTargetFile;
import org.syncany.tests.scenarios.framework.ChangeTypeFolderToSymlinkWithTargetFolder;
import org.syncany.tests.scenarios.framework.ChangeTypeSymlinkWithNonExistingTargetToFile;
import org.syncany.tests.scenarios.framework.ChangeTypeSymlinkWithNonExistingTargetToFolder;
import org.syncany.tests.scenarios.framework.ChangeTypeSymlinkWithTargetFileToFile;
import org.syncany.tests.scenarios.framework.ChangeTypeSymlinkWithTargetFileToFolder;
import org.syncany.tests.scenarios.framework.ChangeTypeSymlinkWithTargetFolderToFile;
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
import org.syncany.tests.scenarios.framework.MoveFileToOtherFolder;
import org.syncany.tests.scenarios.framework.MoveFileWithinFolder;
import org.syncany.tests.scenarios.framework.MoveFolderToOtherFolder;
import org.syncany.tests.scenarios.framework.MoveFolderWithinFolder;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

/**
 * This test case tries to implement all possible file change scenarios
 * using the classes in the framework.* package. An [x] marks implemented 
 * tests.
 * 
 * Attributes:
 * [x] size
 * [x] type
 * [x] content (without size-change)
 * [x] name
 * [x] path
 * [x] permissions (Linux / Windows)
 * [x] last modified date
 * 
 * Actions:
 * [x] create file
 * [x] move file
 * [x] change file without changing size
 * [x] change file with changing size
 * [x] change file type - file to folder
 * [x] change file type - file to symlink
 * [x] change file type - folder to file
 * [x] change file type - folder to symlink
 * [x] delete file
 * [x] create folder
 * [x] move folder
 * [x] delete folder
 * [x] move file to subfolder
 * [x] move folder to subfolder
 * [x] create symlink folder
 * [x] change symlink folder target
 * [x] delete symlink folder
 * [x] file permission denied
 * [x] file is locked

 * file vanishes during index process
 * folder vanishes during index process
 * 
 * file is changed during sync down operation 
 * file is changed during sync up operation
 * 
 */		
public class AllFilePossibilitiesScenarioTest {	
	@Test
	public void testAllPossibilities() throws Exception {		
		final Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		final TestClient clientA = new TestClient("A", testConnection);
		final TestClient clientB = new TestClient("B", testConnection);
		
		ClientActions.run(clientA,
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
				new ChangeTypeSymlinkWithNonExistingTargetToFile(),
				new ChangeTypeSymlinkWithTargetFileToFile(),
				new ChangeTypeSymlinkWithTargetFolderToFile(),
				new ChangeTypeSymlinkWithNonExistingTargetToFolder(),
				new ChangeTypeSymlinkWithTargetFileToFolder(),
				new ChangeTypeSymlinkWithTargetFolderToFolder(),
				new ChangeTypeFolderToFile(), 
				new ChangeTypeFolderToSymlinkWithNonExistingTarget(),
				new ChangeTypeFolderToSymlinkWithTargetFile(),
				new ChangeTypeFolderToSymlinkWithTargetFolder(),
				new CreateFile(),
				new CreateFolder(),
				new CreateSymlinkToFile(),
				new CreateSymlinkToFolder(),
				new CreateSymlinkToNonExisting(),
				new DeleteFile(),
				new DeleteFolder(),				
				new MoveFileToOtherFolder(),
				new MoveFileWithinFolder(),
				new MoveFolderToOtherFolder(),
				new MoveFolderWithinFolder(),
				
				//new LockFile() and new UnlockFile() are not compatible with these tests
			},
			new Executable() {
				@Override
				public void execute() throws Exception {
					clientA.upWithForceChecksum();		
					
					clientB.down();
					assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
					assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());					
				}			
			}
		);
		
		clientA.deleteTestData();
		clientB.deleteTestData();
	}
	
	@Test
	public void testChangeTypeSymlinkWithTargetFileToFolder() throws Exception {		
		final Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		final TestClient clientA = new TestClient("A", testConnection);
		final TestClient clientB = new TestClient("B", testConnection);
		
		ClientActions.run(clientA, null, new CreateFileTree(), null);
		ClientActions.run(clientA, null, new ChangeTypeFileToSymlinkWithTargetFolder(), null);
		
		clientA.upWithForceChecksum();		
		clientB.down();
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());					
		
		clientA.deleteTestData();
		clientB.deleteTestData();
	}
	
}
