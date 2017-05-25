/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.syncany.util.EnvironmentUtil.OperatingSystem;

public class NormalizedPath {
	protected static final Logger logger = Logger.getLogger(NormalizedPath.class.getSimpleName());	
	private static final Pattern ILLEGAL_CHARS_PATTERN_WINDOWS = Pattern.compile("[\\\\/:*?\"<>|\0]");
	private static final Pattern ILLEGAL_CHARS_PATTERN_UNIX_LIKE = Pattern.compile("[\0]");
	private static final Pattern ILLEGAL_NON_ASCII_CHARS_PATTERN = Pattern.compile("[^a-zA-Z0-9., ]");
	
	protected File root;
	protected String normalizedPath;
	
	public NormalizedPath(File root, String normalizedPath) {
		this.root = root;
		this.normalizedPath = normalizedPath;
	}
	
	@Override
	public String toString() {
		return normalizedPath;
	}
	
	public NormalizedPath getParent() {
		int lastIndexOfSlash = normalizedPath.lastIndexOf("/");
		
		if (lastIndexOfSlash == -1) {
			return new NormalizedPath(root, "");
		}
		else {
			return new NormalizedPath(root, normalizedPath.substring(0, lastIndexOfSlash));
		}
	}	

	private List<String> getParts() {
		return Arrays.asList(normalizedPath.split("[/]"));
	}		
	
	public File toFile() {
		if (root != null) {
			return new File(root, normalizedPath);
		}
		else {
			return new File(normalizedPath);
		}
	}
	
	private boolean canCreate(String pathPart) {
		try {
			Path tempFile = Files.createTempFile(pathPart, "canCreate");
			Files.deleteIfExists(tempFile);

			return true;
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "WARNING: Cannot create file: "+pathPart);
			return false;
		}
	}
	
	public boolean hasIllegalChars() {
		return hasIllegalChars(normalizedPath);
	}
	
	private String getExtension(boolean includeDot) {
		return getExtension(normalizedPath, includeDot);
	}
	
	private String getExtension(String filename, boolean includeDot) {
		int lastDot = filename.lastIndexOf(".");
		int lastSlash = filename.lastIndexOf("/");

		if (lastDot == -1 || lastSlash > lastDot) {
			return "";
		}

		String extension = filename.substring(lastDot + 1, filename.length());
		return (includeDot) ? "." + extension : extension;
	}
	
	private String getPathWithoutExtension(String filename) {
		String extension = getExtension(true); // .txt
		
		if ("".equals(extension)) {
			return filename;
		}
		else {
			return filename.substring(0, filename.length() - extension.length());
		}
	}
	
	private boolean hasIllegalChars(String pathPart) {
		if (EnvironmentUtil.isWindows() && ILLEGAL_CHARS_PATTERN_WINDOWS.matcher(pathPart).find()) {
			return true;
		}		
		else if (EnvironmentUtil.isUnixLikeOperatingSystem() && ILLEGAL_CHARS_PATTERN_UNIX_LIKE.matcher(pathPart).find()) {
			return true;
		}
		else {
			return false;
		}
	}
	
	private String cleanIllegalChars(String pathPart) {
		if (EnvironmentUtil.isWindows()) {
			return ILLEGAL_CHARS_PATTERN_WINDOWS.matcher(pathPart).replaceAll("");						
		}
		else {
			return ILLEGAL_CHARS_PATTERN_UNIX_LIKE.matcher(pathPart).replaceAll("");
		}
	}	
	
	private String cleanAsciiOnly(String pathPart) {
		return ILLEGAL_NON_ASCII_CHARS_PATTERN.matcher(pathPart).replaceAll("");
	}	
	
	private String addFilenameConflictSuffix(String pathPart, String filenameSuffix) {
		String conflictFileExtension = getExtension(pathPart, false);		
		boolean originalFileHasExtension = conflictFileExtension != null && !"".equals(conflictFileExtension);

		if (originalFileHasExtension) {
			String conflictFileBasename = getPathWithoutExtension(pathPart);
			return String.format("%s (%s).%s", conflictFileBasename, filenameSuffix, conflictFileExtension);						
		}
		else {
			return String.format("%s (%s)", pathPart, filenameSuffix);
		}
	}

	public NormalizedPath withSuffix(String filenameSuffix, boolean canExist) throws Exception {
		if (canExist) {
			return toCreatable(filenameSuffix, 0);
		}
		else {
			NormalizedPath creatableNormalizedPath = null;
			int attempt = 0;
			
			do {
				String aFilenameSuffix = (attempt > 0) ? filenameSuffix + " " + attempt : filenameSuffix;
				creatableNormalizedPath = new NormalizedPath(root, addFilenameConflictSuffix(normalizedPath.toString(), aFilenameSuffix));
				boolean exists = FileUtil.exists(creatableNormalizedPath.toFile());
				
				if (!exists) {
					return creatableNormalizedPath;
				}
			} while (attempt++ < 200);
			
			throw new Exception("Cannot create path with suffix; "+attempt+" attempts: "+creatableNormalizedPath);
		}
	}
	
	/*     pictures/
	 *       some/
	 *         folder/
	 *           file.jpg
	 *       some\\folder/
	 * ->       file.jpg
	 * 
	 *  relativeNormalizedPath = pictures/some\\folder/file.jpg
	 *  
	 *  -> createable: pictures/somefolder (filename conflict)/file.jpg
	 *  
	 *  http://msdn.microsoft.com/en-us/library/system.io.path.getinvalidfilenamechars.aspx
	 */	
	public NormalizedPath toCreatable(String filenameSuffix, boolean canExist) throws Exception {		
		if (canExist) {
			return toCreatable(filenameSuffix, 0);
		}
		else {
			NormalizedPath creatableNormalizedPath = null;
			int attempt = 0;
			
			do {
				creatableNormalizedPath = toCreatable(filenameSuffix, attempt);
				boolean exists = FileUtil.exists(creatableNormalizedPath.toFile());
				
				// TODO [medium] The exists-check should be in the pathPart-loop, b/c what if fileB is a FILE in this path: folderA/fileB/folderC/file1.jpg
				
				if (!exists) {
					return creatableNormalizedPath;
				}
				
				logger.log(Level.WARNING, " - File exists, trying new file: " + creatableNormalizedPath.toFile());
			} while (attempt++ < 10);
			
			throw new Exception("Cannot create creatable path; "+creatableNormalizedPath+" attempts: "+attempt);
		}
	}
	
	private NormalizedPath toCreatable(String filenameSuffix, int attempt) {		
		List<String> cleanedRelativePathParts = new ArrayList<String>();
		String attemptedFilenameSuffix = (attempt > 0) ? filenameSuffix + " " + attempt : filenameSuffix;
		
		for (String pathPart : getParts()) {
			boolean needsCleansing = false;
			
			// Determine if path part is illegal
			if (hasIllegalChars(pathPart)) {
				needsCleansing = true;
			}
			else {
				try {
					Paths.get(pathPart);
				}
				catch (InvalidPathException e) {
					needsCleansing = true;
				}
			}
			
			// Clean if it is illegal
			if (needsCleansing) {				
				String cleanedParentPart = addFilenameConflictSuffix(cleanIllegalChars(pathPart), attemptedFilenameSuffix); // TODO [low] attempt does not make sense hree
				
				// Check if cleaned path actually can be created (creates local file!)
				if (canCreate(cleanedParentPart)) {
					pathPart = cleanedParentPart;
				}
				else {
					pathPart = addFilenameConflictSuffix(cleanAsciiOnly(pathPart), attemptedFilenameSuffix); // TODO [low] attempt does not make sense hree
				}				
				
				logger.log(Level.INFO, "       + WAS ILLEGAL: Now: "+pathPart);
			}
			
			// Add to path part list
			cleanedRelativePathParts.add(pathPart);
		}
		
		String cleanedRelativeTargetPath = StringUtil.join(cleanedRelativePathParts, File.separator);
		return new NormalizedPath(root, cleanedRelativeTargetPath);		
	}
}	
