package org.syncany.tests.scenarios;

import static org.junit.Assert.*;

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
		 * 
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
		 * (create symlink)
		 * (change symlink target)
		 * (delete symlink)
		 * 
		 * file vanishes during index process
		 * folder vanies during index process
		 * 
		 * file is changed during sync down operation 
		 * 
		 * 
		 */
		
		
		fail("Nothing here yet.");
	}
}
