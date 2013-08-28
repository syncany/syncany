package org.syncany.tests.util;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.junit.internal.ArrayComparisonFailure;
import org.syncany.util.StringUtil;

public class TestAssertUtil {
	public static void assertCollectionEquals(String message, Collection<? extends Object> expected, Collection<? extends Object> actual) {
		assertEquals(message+": Different amount of objects.", expected.size(), actual.size());
		
		Iterator<? extends Object> expectedIt = expected.iterator();
		Iterator<? extends Object> actualIt = actual.iterator();
		
		int i = 0;
		while (expectedIt.hasNext()) {			
			Object expectedObj = expectedIt.next();			
			Object actualObj = actualIt.next();
			
			assertEquals(message+": actual["+i+"] differs from expected["+i+"]: ", expectedObj, actualObj);
			i++;
		}
	}

	public static void assertFileListEquals(Map<String, File> expectedFiles, Map<String, File> actualFiles) throws ArrayComparisonFailure, Exception {
		assertFileListEquals("File list does not match", expectedFiles, actualFiles);
	}
	
	public static void assertFileListEquals(String message, Map<String, File> expectedFiles, Map<String, File> actualFiles) throws ArrayComparisonFailure, Exception {
		assertCollectionEquals("Actual file list ("+actualFiles.size()+" entries) differs from expected file list ("+expectedFiles.size()+" entries)", expectedFiles.keySet(), actualFiles.keySet());

		for (Map.Entry<String, File> expectedFileEntry : expectedFiles.entrySet()) {
			File expectedFile = expectedFileEntry.getValue();
			File actualFile = actualFiles.remove(expectedFileEntry.getKey());
			
			assertFileEquals(message+": File not equal", expectedFile, actualFile);
		}		
	}
	
	public static void assertFileEquals(File expectedFile, File actualFile) throws ArrayComparisonFailure, Exception {
		assertFileEquals("Files are not equal", expectedFile, actualFile);
	}
	
	public static void assertFileEquals(String message, File expectedFile, File actualFile) throws ArrayComparisonFailure, Exception {
		if (expectedFile == null && actualFile == null) {
			return;
		}
		
		assertNotNull(message+": Actual file is "+actualFile+", expected file is null.", expectedFile);
		assertNotNull(message+": Expected file is "+expectedFile+", actual file is null.", actualFile);

		if (!expectedFile.exists()) {
			fail(message+": Expected file "+expectedFile+" does not exist.");
		}
		
		if (!actualFile.exists()) {
			fail(message+": Actual file "+actualFile+" does not exist.");
		}

		if (actualFile.isDirectory() != expectedFile.isDirectory()) {
			fail(message+" Comparing a directory with a file (actual is dir = "+actualFile.isDirectory()+", expected is dir = "+expectedFile.isDirectory()+")");
		}
		
		if (actualFile.isDirectory() && expectedFile.isDirectory()) {
			return;
		}
		
		if (actualFile.length() != expectedFile.length()) {
			fail(message+": Actual file size ("+actualFile.length()+") does not match expected file size ("+expectedFile.length()+")");
		}
		
		byte[] expectedFileChecksum = TestFileUtil.createChecksum(expectedFile);
		byte[] actualFileChecksum = TestFileUtil.createChecksum(actualFile);
		
		assertArrayEquals(message+": Actual file checksum ("+StringUtil.toHex(actualFileChecksum)+") and expected file checksum ("+StringUtil.toHex(expectedFileChecksum)+") do not match.", expectedFileChecksum, actualFileChecksum);		
	}
}
