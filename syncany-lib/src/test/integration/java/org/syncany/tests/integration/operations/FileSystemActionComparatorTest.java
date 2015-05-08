/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.integration.operations;

import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileType;
import org.syncany.operations.down.FileSystemActionComparator;
import org.syncany.operations.down.actions.DeleteFileSystemAction;
import org.syncany.operations.down.actions.FileSystemAction;
import org.syncany.operations.down.actions.NewFileSystemAction;
import org.syncany.operations.down.actions.RenameFileSystemAction;
import org.syncany.tests.util.TestConfigUtil;

public class FileSystemActionComparatorTest {
	// TODO [low] write more unit tests for FileSystemActionComparator
	
	@Test
	public void testFileSystemActionComparator() throws Exception {
		// Setup
		List<FileSystemAction> actions = new ArrayList<FileSystemAction>();
		
		actions.add(createNewFileSystemAction("deletedfolderXX", FileType.FILE));  
		actions.add(createNewFileSystemAction("newsymlink", FileType.SYMLINK));
		actions.add(createNewFileSystemAction("NEWfolder", FileType.FOLDER));
		actions.add(createNewFileSystemAction("newfile.jpg", FileType.FILE));
		actions.add(createDeleteFileSystemAction("deletedfolderXX", FileType.FOLDER)); // << same as folder above!
		actions.add(createDeleteFileSystemAction("deletedsymlink.jpg", FileType.SYMLINK));
		actions.add(createNewFileSystemAction("newfile2.jpg", FileType.FILE));
		actions.add(createRenameFileSystemAction("from.jpg", "to.jpg", FileType.FILE));
		actions.add(createDeleteFileSystemAction("deletedfile2.jpg", FileType.FILE));
		
		// Run
		FileSystemActionComparator actionComparator = new FileSystemActionComparator();		
		actionComparator.sort(actions);
		
		// Test
		assertArrayEquals("Actions should match order", 
			new String[] {
				"DeleteFileSystemAction,deletedfile2.jpg,FILE",
				"DeleteFileSystemAction,deletedsymlink.jpg,SYMLINK",
				"NewFileSystemAction,NEWfolder,FOLDER",
				"NewFileSystemAction,newfile.jpg,FILE",
				"NewFileSystemAction,newfile2.jpg,FILE",
				"NewFileSystemAction,newsymlink,SYMLINK",
				"RenameFileSystemAction,to.jpg,FILE",
				"DeleteFileSystemAction,deletedfolderXX,FOLDER",
				"NewFileSystemAction,deletedfolderXX,FILE"    // <<< moved here by postCompareSort!						
			}, 
			toArray(actions)
		);
		
		System.out.println(actions);
	}
	
	private DeleteFileSystemAction createDeleteFileSystemAction(String path, FileType type) throws Exception {
		FileVersion firstFileVersion = createFileVersion(path, type);
		FileVersion secondFileVersion = createFileVersion(path, type, firstFileVersion);
		
		return new DeleteFileSystemAction(createDummyConfig(), firstFileVersion, secondFileVersion, null);
	}	

	private NewFileSystemAction createNewFileSystemAction(String path, FileType type) throws Exception {
		FileVersion firstFileVersion = createFileVersion(path, type);
		return new NewFileSystemAction(createDummyConfig(), firstFileVersion, null);
	}
	
	private RenameFileSystemAction createRenameFileSystemAction(String fromPath, String toPath, FileType type) throws Exception {
		FileVersion firstFileVersion = createFileVersion(fromPath, type);
		FileVersion secondFileVersion = createFileVersion(toPath, type, firstFileVersion);
		
		return new RenameFileSystemAction(createDummyConfig(), firstFileVersion, secondFileVersion, null);
	}

	private FileVersion createFileVersion(String path, FileType type) {
		return createFileVersion(path, type, null);
	}
	
	private FileVersion createFileVersion(String path, FileType type, FileVersion basedOnFileVersion) {
		if (basedOnFileVersion == null) {
			FileVersion fileVersion = new FileVersion();
			fileVersion.setPath(path);
			fileVersion.setType(type);
			fileVersion.setVersion(1L);
			
			return fileVersion;
		}
		else {
			FileVersion fileVersion = basedOnFileVersion.clone();
			fileVersion.setPath(path);
			fileVersion.setType(type);
			fileVersion.setVersion(basedOnFileVersion.getVersion()+1);
			
			return fileVersion;
		}			
	}		

	private Config createDummyConfig() throws Exception {
		return TestConfigUtil.createDummyConfig();
	}
	
	private String[] toArray(List<FileSystemAction> actions) {
		String[] actionStrArr = new String[actions.size()];
		
		for (int i=0; i<actions.size(); i++) {
			FileSystemAction action = actions.get(i);
			
			actionStrArr[i] = 
				  action.getClass().getSimpleName()
				+ "," 
				+ actions.get(i).getFile2().getPath()
				+ ","
				+ actions.get(i).getType();
			
			System.out.println("actual["+i+"]: "+actionStrArr[i]);
		}
		
		return actionStrArr;
	}
}
