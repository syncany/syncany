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
package org.syncany.tests.unit.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.util.EnvironmentUtil;
import org.syncany.util.EnvironmentUtil.OperatingSystem;
import org.syncany.util.FileUtil;
import org.syncany.util.NormalizedPath;

public class NormalizedPathTest {
	private OperatingSystem originalOperatingSystem;
	
	@Before
	public void storeOperatingSystem() {
		originalOperatingSystem = EnvironmentUtil.getOperatingSystem();
	}
	
	@After
	public void resetOperatingSystem() {
		// Important: Restore the actual operating systems, 
		//            or other tests might fail.
		
		EnvironmentUtil.setOperatingSystem(originalOperatingSystem);
	}
	
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
	
	@Test
	public void testNameAndParentPathForNormalizedPathsOnWindows() {		
		testNameAndParentPathForNormalizedPaths(OperatingSystem.WINDOWS);
	}
	
	@Test
	public void testNameAndParentPathForNormalizedPathsOnUnixLikeSystems() {		
		testNameAndParentPathForNormalizedPaths(OperatingSystem.UNIX_LIKE);
	}

	private void testNameAndParentPathForNormalizedPaths(OperatingSystem operatingSystem) {		
		EnvironmentUtil.setOperatingSystem(operatingSystem);
		
		// Test 1: For a file called 'A black\white telephone ☎.jpg' 
		//         Note: "A black" is NOT a directory, it's part of the filename (invalid on Windows!)		
		String alreadyNormalizedRelativePathFileStr = "Pictures/A black\\white telephone ☎.jpg";
		NormalizedPath normalizedPathFile = new NormalizedPath(null, alreadyNormalizedRelativePathFileStr);
		
		assertEquals("Pictures/A black\\white telephone ☎.jpg", normalizedPathFile.toString());
		assertEquals("Pictures", normalizedPathFile.getParent().toString());

		// Test 2: For directory called 'black\\white telephones ☎' 		
		String alreadyNormalizedRelativePathDirStr = "Pictures/black\\white telephones ☎";
		NormalizedPath normalizedPathDir = new NormalizedPath(null, alreadyNormalizedRelativePathDirStr);
		
		assertEquals("Pictures/black\\white telephones ☎", normalizedPathDir.toString());
		assertEquals("Pictures", normalizedPathDir.getParent().toString());
		
		// Test 3: For directory called 'black\\white telephones ☎' 		
		String alreadyNormalizedRelativePathFileWithBackslashesDirStr = "Pictures/Black\\White Pictures/Mostly\\Black Pictures/blacky.jpg";
		NormalizedPath normalizedPathWithBackslashesDir = new NormalizedPath(null, alreadyNormalizedRelativePathFileWithBackslashesDirStr);
		
		assertEquals("Pictures/Black\\White Pictures/Mostly\\Black Pictures/blacky.jpg", normalizedPathWithBackslashesDir.toString());
		assertEquals("Pictures/Black\\White Pictures/Mostly\\Black Pictures", normalizedPathWithBackslashesDir.getParent().toString());		
	}
	
	@Test
	public void testNameAndParentPathForNormalizedPathsMoreTests() {		
		// Does not depend on OS
		
		assertEquals("", new NormalizedPath(null, "Philipp").getParent().toString()); 		
	}
	
	@Test
	public void testNormalizationOnWindows() {		
		EnvironmentUtil.setOperatingSystem(OperatingSystem.WINDOWS);
		
		assertEquals("C:/Philipp", NormalizedPath.get(null, "C:\\Philipp\\").toString());
		assertEquals("C:/Philipp", NormalizedPath.get(null, "C:\\Philipp").toString());
		assertEquals("C:/Philipp/image.jpg", NormalizedPath.get(null, "C:\\Philipp\\image.jpg").toString());
		assertEquals("C:/Philipp/image", NormalizedPath.get(null, "C:\\Philipp\\image").toString());
		assertEquals("C:/Philipp/file:with:colons.txt", NormalizedPath.get(null, "C:\\Philipp\\file:with:colons.txt").toString()); // Cannot happen on Windows 
		assertEquals("C:/Philipp/file/with/backslashes.txt", NormalizedPath.get(null, "C:\\Philipp\\file\\with\\backslashes.txt").toString()); 
		assertEquals("C:/Philipp/folder/with/backslashes", NormalizedPath.get(null, "C:\\Philipp\\folder\\with\\backslashes\\").toString()); 
	}
	
	@Test
	public void testCreatablizationOnWindows() throws Exception {		
		EnvironmentUtil.setOperatingSystem(OperatingSystem.WINDOWS);
		File root = new File("C:\\Philipp");
		
		assertEquals("Philipp", new NormalizedPath(root, "Philipp").toCreatable("filename conflict", true).toString());
		assertEquals("Philipp", new NormalizedPath(root, "Philipp").toCreatable("filename conflict", true).toString());
		assertEquals("Philipp/image.jpg", new NormalizedPath(root, "Philipp/image.jpg").toCreatable("filename conflict", true).toString());
		assertEquals("Philipp/image", new NormalizedPath(root, "Philipp/image").toCreatable("filename conflict", true).toString());
		assertEquals("Philipp/filewithcolons (filename conflict).txt", new NormalizedPath(root, "Philipp/file:with:colons.txt").toCreatable("filename conflict", true).toString()); // Cannot happen on Windows 
		assertEquals("Philipp/filewithbackslashes (filename conflict).txt", new NormalizedPath(root, "Philipp/file\\with\\backslashes.txt").toCreatable("filename conflict", true).toString()); 
		assertEquals("Philipp/folderwithbackslashes (filename conflict)", new NormalizedPath(root, "Philipp/folder\\with\\backslashes").toCreatable("filename conflict", true).toString()); 
	}
	
	@Test
	public void testNormalizationOnUnixLikeSystems() {
		EnvironmentUtil.setOperatingSystem(OperatingSystem.UNIX_LIKE);

		assertEquals("/home/philipp", NormalizedPath.get(null, "/home/philipp/").toString());
		assertEquals("/home/philipp", NormalizedPath.get(null, "/home/philipp").toString());
		assertEquals("/home/philipp/image.jpg", NormalizedPath.get(null, "/home/philipp/image.jpg").toString());
		assertEquals("/home/philipp/image", NormalizedPath.get(null, "/home/philipp/image").toString());
		assertEquals("/home/philipp/file:with:colons", NormalizedPath.get(null, "/home/philipp/file:with:colons").toString());
		assertEquals("/home/philipp/file\\with\\backslashes.txt", NormalizedPath.get(null, "/home/philipp/file\\with\\backslashes.txt").toString());
		assertEquals("/home/philipp/folder\\with\\backslashes", NormalizedPath.get(null, "/home/philipp/folder\\with\\backslashes/").toString());		
	}	
	
	@Test
	public void testNormalizationHasIllegalChars() {
		EnvironmentUtil.setOperatingSystem(OperatingSystem.UNIX_LIKE);

		assertFalse(NormalizedPath.get(null, "/home/philipp/").hasIllegalChars());
		assertFalse(NormalizedPath.get(null, "/home/black\\white telephones ☎.txt").hasIllegalChars());
		assertTrue(NormalizedPath.get(null, "/home/philipp/\0.txt").hasIllegalChars());
		
		EnvironmentUtil.setOperatingSystem(OperatingSystem.WINDOWS);

		assertFalse(NormalizedPath.get(null, "C:\\home\\philipp\\").hasIllegalChars());
		assertFalse(NormalizedPath.get(null, "C:\\home\\black-white telephones ☎.txt").hasIllegalChars());
		assertTrue(NormalizedPath.get(null, "C:\\home\\\0.txt").hasIllegalChars());
	}	
}
