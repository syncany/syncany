/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
