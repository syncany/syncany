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
package org.syncany.operations.actions;

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

import org.syncany.util.EnvironmentUtil;
import org.syncany.util.EnvironmentUtil.OperatingSystem;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class NormalizedPath {
	protected static final Logger logger = Logger.getLogger(NormalizedPath.class.getSimpleName());
	
	protected File root;
	protected String normalizedPath;
	
	public NormalizedPath(File root, String normalizedPath) {
		this.root = root;
		this.normalizedPath = normalizedPath;
	}		
	
	public static NormalizedPath get(File root, String relativePath) {
		return get(root, relativePath, EnvironmentUtil.getOperatingSystem());
	}
	
	public static NormalizedPath get(File root, String relativePath, OperatingSystem operatingSystem) {
		if (operatingSystem == OperatingSystem.WINDOWS) {
			return new NormalizedPath(root, relativePath.replaceAll("\\\\$",  "").replaceAll("\\\\", "/"));
		}
		else {
			return new NormalizedPath(root, relativePath.replaceAll("/$", ""));
		}
	}
	
	@Override
	public String toString() {
		return normalizedPath;
	}
	
	public NormalizedPath getName() {
		int lastIndexOfSlash = normalizedPath.lastIndexOf("/");
		
		if (lastIndexOfSlash == -1) {
			return this;
		}
		else {
			return new NormalizedPath(root, normalizedPath.substring(lastIndexOfSlash+1));
		}
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

	public List<String> getParts() {
		return Arrays.asList(normalizedPath.split("[/]"));
	}		
	
	public List<NormalizedPath> getParents() {
		List<NormalizedPath> parents = new ArrayList<NormalizedPath>();
		List<String> parts = Arrays.asList(normalizedPath.split("[/]"));
		
		if (parts.size() > 0) {
			NormalizedPath previousNormalizedParent = null;
			
			for (int i=0; i<parts.size(); i++) {
				String normalizedParentPath = (previousNormalizedParent != null) ? previousNormalizedParent.toString() + "/" + parts.get(i) : parts.get(i);
				NormalizedPath parent = new NormalizedPath(root, normalizedParentPath);
				
				parents.add(parent);
				previousNormalizedParent = parent;
			}			
		}
		
		return parents;
	}		
	
	public File toFile() {
		if (root != null) {
			return new File(root+File.separator+normalizedPath);
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
	
	public String getExtension(boolean includeDot) {
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
	
	public String getBasename() {
		return getBasename(normalizedPath);
	}
	
	private String getBasename(String filename) {
		String extension = getExtension(true); // .txt
		
		if ("".equals(extension)) {
			return filename;
		}
		else {
			return filename.substring(0, filename.length() - extension.length());
		}
	}
	
	private boolean hasIllegalChars(String pathPart) {
		if (EnvironmentUtil.isWindows() && pathPart.matches(".*[\\\\:*?\"<>|].*")) {
			return true;
		}		
		else {
			return false;
		}
	}
	
	private String cleanIllegalChars(String pathPart) {
		if (EnvironmentUtil.isWindows()) {
			return pathPart.replaceAll("[\\\\/:*?\"<>|]","");						
		}
		else {
			return pathPart.replaceAll("[/]","");
		}
	}	
	
	private String cleanAsciiOnly(String pathPart) {
		return pathPart.replaceAll("[^a-zA-Z0-9., ]","");
	}	
	
	private String addFilenameConflictSuffix(String pathPart, String filenameSuffix) {
		String conflictFileExtension = getExtension(pathPart, false);		
		boolean originalFileHasExtension = conflictFileExtension != null && !"".equals(conflictFileExtension);

		if (originalFileHasExtension) {
			String conflictFileBasename = getBasename(pathPart);
			return String.format("%s (%s).%s", conflictFileBasename, filenameSuffix, conflictFileExtension);						
		}
		else {
			return String.format("%s (%s)", pathPart, filenameSuffix);
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
			
			while (attempt++ < 10) {
				creatableNormalizedPath = toCreatable(filenameSuffix, attempt);
				boolean exists = FileUtil.exists(creatableNormalizedPath.toFile());
				
				if (!exists) {
					return creatableNormalizedPath;
				}
			} 
			
			throw new Exception("Cannot create creatable path; "+attempt+" attempts: "+creatableNormalizedPath);
		}
	}
	
	private NormalizedPath toCreatable(String filenameSuffix, int attempt) {		
		List<String> cleanedRelativePathParts = new ArrayList<String>();
		filenameSuffix = (attempt > 0) ? filenameSuffix + " " + attempt : filenameSuffix;
		
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
				String cleanedParentPart = addFilenameConflictSuffix(cleanIllegalChars(pathPart), filenameSuffix);
				
				if (canCreate(cleanedParentPart)) {
					pathPart = cleanedParentPart;
				}
				else {
					pathPart = addFilenameConflictSuffix(cleanAsciiOnly(pathPart), filenameSuffix);
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
