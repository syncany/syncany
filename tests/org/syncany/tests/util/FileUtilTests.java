package org.syncany.tests.util;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;
import org.syncany.util.FileUtil;

public class FileUtilTests {
	@Test
	public void testGetRelativeFilePath() {
		String expectedResult = "somefile";
		
		File[] rootFolders = new File[] {
			new File("/home/user/Syncany"),
			new File("/home/user/Syncany/"),
			new File("/home/user/Syncany//"),
			new File("/home/user//Syncany"),
			new File("/home/user//Syncany/"),
			new File("/home/user//Syncany//")			
		};
		
		File[] files = new File[] {
			new File("/home/user/Syncany/somefile"),
			new File("/home/user/Syncany/somefile/"),
			new File("/home/user/Syncany/somefile//"),
			new File("/home/user/Syncany//somefile"),
			new File("/home/user/Syncany//somefile/"),
			new File("/home/user/Syncany//somefile//")
		};
		
		for (File rootFolder : rootFolders) {
			for (File file : files) {
				String actualResult = FileUtil.getRelativePath(rootFolder, file);
				assertEquals("Expected '"+expectedResult+"' for root folder '"+rootFolder+"' and file '"+file+"'", expectedResult, actualResult);
			}
		}
	}
	
	@Test
	public void testGetRelativeFilePathSpecialCases() {
		assertEquals("", FileUtil.getRelativePath(new File("/home/user/"), new File("/home/user")));
		assertEquals("", FileUtil.getRelativePath(new File("/home/user/"), new File("/home/user/")));
		assertEquals("", FileUtil.getRelativePath(new File("/home/user/"), new File("/home/user//")));
		
	}
	
}
