package org.syncany.tests.scenarios;

import org.junit.Test;

public class AllFilePossibilitiesScenarioTest {
	@Test
	public void testAllPossibilities() {
		/**
		 * attributes:
		 * - size
		 * - type
		 * - content
		 * - name
		 * - path
		 * - permissions (linux / windows)
		 * 
		 * create file
		 * move file
		 * change file without changing size
		 * change file with changing size
		 * change file type - folder to file
		 * change file type - file to folder
		 * (change file type - folder to symlink)
		 * (change file type - file to symlink)
		 * delete file
		 * 
		 * create folder
		 * move folder
		 * delete folder
		 * 
		 * move file to subfolder
		 * move folder to subfolder
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
	}
}
