package org.syncany.tests.util;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.Map;

import org.junit.Test;
import org.syncany.util.ClasspathUtil;

public class ClasspathUtilTest {
	@Test
	public void testGetClasspathClasses() {
		// Prepare
		String[] testClasses = new String[] {
			"org.syncany.Client",
			"org.syncany.config.Config",
			"org.syncany.database.Database"
		};
				
		// Run!
		Map<URL, String> classes = ClasspathUtil.getClasspathClasses();
		
		// Test
		assertNotNull(classes);
		
		for (String testClass : testClasses) {
			assertTrue("Classes list should contain class "+testClass, classes.values().contains(testClass));
		}
	}
	
	@Test
	public void testGetClasspathPackages() {
		// Prepare
		String[] testPackages = new String[] {
			"org.syncany",
			"org.syncany.config",
			"org.syncany.database"
		};
				
		// Run!
		Map<URL, String> packages = ClasspathUtil.getClasspathPackages();
		
		// Test
		assertNotNull(packages);
		
		for (String testPackage : testPackages) {
			assertTrue("Classes list should contain package "+testPackage, packages.values().contains(testPackage));
		}		
	}	
	
	@Test
	public void testGetClasspathEntries() {
		// Prepare
		String[] testEntries = new String[] {
			// Packages
			"org.syncany",
			"org.syncany.config",
			"org.syncany.database",
			
			// Classes
			"org.syncany.Client",
			"org.syncany.config.Config",
			"org.syncany.database.Database"
		};
		
		// Run!
		Map<URL, String> entries = ClasspathUtil.getClasspathEntries();
		
		// Test
		assertNotNull(entries);
		
		for (String testEntry : testEntries) {
			assertTrue("Classes list should contain entry "+testEntry, entries.values().contains(testEntry));
		}		
	}
	
}
