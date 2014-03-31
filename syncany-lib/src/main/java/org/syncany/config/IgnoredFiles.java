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
package org.syncany.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class serves as a container of patterns of filenames
 * that should be ignored.
 * 
 * @author Pim Otte 
 */
public class IgnoredFiles {
	private static final Logger logger = Logger.getLogger(ConfigHelper.class.getSimpleName());	
	private Set<String> ignorePatterns;
	private Set<String> ignorePaths;
	private File ignoreFile;
	
	public IgnoredFiles(File ignoreFile) {
		ignorePatterns = new HashSet<String>();
		ignorePaths = new HashSet<String>();
		this.ignoreFile = ignoreFile;
		loadPatterns();
	}
	
	/**
	 * Method to check whether a file should be ignored.
	 * Should only be called at indexing time.
	 */
	public boolean isFileIgnored(String filePath) {
		// Check all exact paths
		for (String path : ignorePaths) {
			if (path.equals(filePath)) {
				return true;
			}
		} 
		
		// Check all regular expressions
		for (String pattern : ignorePatterns) {
			if (filePath.matches(pattern)) {
				return true;
			}
		}
		
		return false;
	}
	
	public void loadPatterns() {
		if (ignoreFile != null && ignoreFile.exists()) {
			try {
				Scanner scanner = new Scanner(ignoreFile);
				
				while (scanner.hasNextLine()) {
					String ignorePattern = scanner.nextLine();
					
					if (!ignorePattern.isEmpty()) {
						if (ignorePattern.startsWith("regex:")) {
							// Chop off regex: indicator
							ignorePatterns.add(ignorePattern.substring(6));
						}
						else {
							ignorePaths.add(ignorePattern);
						}
					}
				}
				scanner.close();
			}
			catch (FileNotFoundException e) {
				logger.log(Level.INFO, "Ignore file not found (existed before).");
			}
		}
		else {
			// In case the ignoreFile has been deleted, reset patterns
			ignorePatterns = new HashSet<String>();
			ignorePaths = new HashSet<String>();
		}
	}
} 
