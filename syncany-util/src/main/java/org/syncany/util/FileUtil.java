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
package org.syncany.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A file utility class
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FileUtil {
	public static String getRelativePath(File base, File file) {
		String relativeFilePath = base.toURI().relativize(file.toURI()).getPath();
		
		if (relativeFilePath.endsWith(File.separator)) {
			relativeFilePath = relativeFilePath.substring(0, relativeFilePath.length() - 1);
		}
		
		return relativeFilePath;
	}
	
	public static String getRelativeDatabasePath(File base, File file) {
		String relativeFilePath = getRelativePath(base, file);
		
		// Note: This is more important than it seems. Unix paths may contain backslashes
		//       so that 'black\white.jpg' is a perfectly valid file path. Windows file names
		//       may never contain backslashes, so that '\' can be safely transformed to the
		//       '/'-separated database path!
		
		if (isWindows()) {
			String databasePath = relativeFilePath.toString().replaceAll("\\\\", "/");
			return removeTrailingSlash(databasePath); 
		}
		else {
			return removeTrailingSlash(relativeFilePath);
		}
	}
	
	public static String removeTrailingSlash(String filename) {
		if ("/".equals(filename.substring(filename.length()-1))) {
			return filename.substring(0, filename.length()-1);
		}
		else {
			return filename;
		}
	}

	public static String getAbsoluteParentDirectory(File file) {
		return file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(File.separator));
	}

	public static String getAbsoluteParentDirectory(String absFilePath) {
		return absFilePath.substring(0, absFilePath.lastIndexOf(File.separator));
	}

	public static String getRelativeParentDirectory(File base, File file) {
		return getRelativePath(base, new File(getAbsoluteParentDirectory(file)));
	}

	public static List<File> getRecursiveFileList(File root) throws FileNotFoundException {
		return getRecursiveFileList(root, false, false);
	}

	public static List<File> getRecursiveFileList(File root, boolean includeDirectories, boolean followSymlinkDirectories)
			throws FileNotFoundException {
		if (!root.isDirectory() || !root.canRead() || !root.exists()) {
			throw new FileNotFoundException("Invalid directory " + root);
		}

		List<File> result = getRecursiveFileListNoSort(root, includeDirectories, followSymlinkDirectories);
		Collections.sort(result);

		return result;
	}

	private static List<File> getRecursiveFileListNoSort(File root, boolean includeDirectories, boolean followSymlinkDirectories) {
		List<File> result = new ArrayList<File>();
		List<File> filesDirs = Arrays.asList(root.listFiles());

		for (File file : filesDirs) {
			boolean isDirectory = file.isDirectory();
			boolean isSymlinkDirectory = isDirectory && FileUtil.isSymlink(file);
			boolean includeFile = !isDirectory || includeDirectories;
			boolean followDirectory = (isSymlinkDirectory && followSymlinkDirectories) || (isDirectory && !isSymlinkDirectory);

			if (includeFile) {
				result.add(file);
			}

			if (followDirectory) {
				List<File> deeperList = getRecursiveFileListNoSort(file, includeDirectories, followSymlinkDirectories);
				result.addAll(deeperList);
			}
		}

		return result;
	}

	/**
	 * Retrieves the extension of a file.
	 * Example: "html" in the case of "/htdocs/index.html"
	 *
	 * @param file
	 * @return
	 */
	public static String getExtension(File file) {
		return getExtension(file.getName(), false);
	}

	public static String getExtension(File file, boolean includeDot) {
		return getExtension(file.getName(), includeDot);
	}

	public static String getExtension(String filename, boolean includeDot) {
		int dot = filename.lastIndexOf(".");

		if (dot == -1) {
			return "";
		}

		return ((includeDot) ? "." : "") + filename.substring(dot + 1, filename.length());
	}

	/**
	 * Retrieves the basename of a file.
	 * Example: "index" in the case of "/htdocs/index.html"
	 * 
	 * @param file
	 * @return
	 */
	public static String getBasename(File file) {
		return getBasename(file.getName());
	}

	public static String getBasename(String filename) {
		int dot = filename.lastIndexOf(".");

		if (dot == -1) {
			return filename;
		}

		return filename.substring(0, dot);
	}

	public static File getCanonicalFile(File file) {
		try {
			return file.getCanonicalFile();
		}
		catch (IOException ex) {
			return file;
		}
	}

	public static void writeToFile(byte[] bytes, File file) throws IOException {
		writeToFile(new ByteArrayInputStream(bytes), file);
	}

	public static void writeToFile(InputStream is, File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);

		int read = 0;
		byte[] bytes = new byte[4096];

		while ((read = is.read(bytes)) != -1) {
			fos.write(bytes, 0, read);
		}

		is.close();
		fos.close();
	}

	public static void appendToOutputStream(File fileToAppend, OutputStream outputStream) throws IOException {
		appendToOutputStream(new FileInputStream(fileToAppend), outputStream);
	}

	public static void appendToOutputStream(InputStream inputStream, OutputStream outputStream) throws IOException {
		byte[] buf = new byte[4096];

		int len;
		while ((len = inputStream.read(buf)) > 0) {
			outputStream.write(buf, 0, len);
		}

		inputStream.close();
	}

	public static byte[] createChecksum(File filename, String digestAlgorithm) throws Exception {
		FileInputStream fis = new FileInputStream(filename);

		byte[] buffer = new byte[1024];
		MessageDigest complete = MessageDigest.getInstance(digestAlgorithm);
		int numRead;

		do {
			numRead = fis.read(buffer);
			if (numRead > 0) {
				complete.update(buffer, 0, numRead);
			}
		} while (numRead != -1);

		fis.close();
		return complete.digest();
	}

	public static String toDatabaseFilePath(String path) {
		return path.replaceAll("\\\\", "/");
	}

	public static boolean isFileLocked(File file) {
		if (!file.exists()) {
			return false;
		}

		if (file.isDirectory()) {
			return false;
		}

		if (isSymlink(file)) {
			return false;
		}

		RandomAccessFile randomAccessFile = null;
		boolean fileLocked = false;

		try {
			// Test 1 missing permissions or locked file parts. If File is not readable == locked
			randomAccessFile = new RandomAccessFile(file, "r");
			randomAccessFile.close();
		}
		catch (Exception e) {
			fileLocked = true;
		}

		if (!fileLocked && file.canWrite()) {
			try {
				// Test 2:Locked file parts
				randomAccessFile = new RandomAccessFile(file, "rw");

				// Test 3: Set lock and release it again
				FileLock fileLock = randomAccessFile.getChannel().tryLock();

				if (fileLock == null) {
					fileLocked = true;
				}
				else {
					try {
						fileLock.release();
					}
					catch (Exception e) { /* Nothing */
					}
				}
			}
			catch (Exception e) {
				fileLocked = true;
			}

			if (randomAccessFile != null) {
				try {
					randomAccessFile.close();
				}
				catch (IOException e) { /* Nothing */
				}
			}
		}

		return fileLocked;
	}

	public static String getName(String fullName) {
		return new File(fullName).getName();
	}

	public static boolean symlinksSupported() {
		return isUnixLikeOperatingSystem();
	}

	public static boolean isUnixLikeOperatingSystem() {
		return File.separatorChar == '/';
	}

	public static boolean isWindows() {
		return File.separatorChar == '\\';
	}

	public static boolean isSymlink(File file) {
		return Files.isSymbolicLink(Paths.get(file.getAbsolutePath()));
	}

	public static String readSymlinkTarget(File file) {
		try {
			return Files.readSymbolicLink(Paths.get(file.getAbsolutePath())).toString();
		}
		catch (IOException e) {
			return null;
		}
	}

	public static void createSymlink(String targetPathStr, File symlinkFile) throws Exception {
		Path targetPath = Paths.get(targetPathStr);
		Path symlinkPath = Paths.get(symlinkFile.getPath());

		Files.createSymbolicLink(symlinkPath, targetPath);
	}

	public static String dosAttrsToString(DosFileAttributes dosAttrs) {
		StringBuilder sb = new StringBuilder();

		sb.append(dosAttrs.isReadOnly() ? "r" : "-");
		sb.append(dosAttrs.isHidden() ? "h" : "-");
		sb.append(dosAttrs.isArchive() ? "a" : "-");
		sb.append(dosAttrs.isSystem() ? "s" : "-");

		return sb.toString();
	}

	public static DosFileAttributes dosAttrsFromString(final String dosAttributes) {
		return new DosFileAttributes() {
			@Override
			public boolean isReadOnly() {
				return dosAttributes.charAt(0) == 'r';
			}

			@Override
			public boolean isHidden() {
				return dosAttributes.charAt(1) == 'h';
			}

			@Override
			public boolean isArchive() {
				return dosAttributes.charAt(2) == 'a';
			}

			@Override
			public boolean isSystem() {
				return dosAttributes.charAt(3) == 's';
			}

			@Override
			public long size() {
				return 0;
			}

			@Override
			public FileTime lastModifiedTime() {
				return null;
			}

			@Override
			public FileTime lastAccessTime() {
				return null;
			}

			@Override
			public boolean isSymbolicLink() {
				return false;
			}

			@Override
			public boolean isRegularFile() {
				return false;
			}

			@Override
			public boolean isOther() {
				return false;
			}

			@Override
			public boolean isDirectory() {
				return false;
			}

			@Override
			public Object fileKey() {
				return null;
			}

			@Override
			public FileTime creationTime() {
				return null;
			}
		};
	}

	/**
	 * Replaces the <tt>exists()</tt> method in the <tt>File</tt> class by taking symlinks into account. 
	 * The method returns <tt>true</tt> if the file exists, <tt>false</tt> otherwise.
	 * 
	 * <p>Note: The method returns <tt>true</tt>, if a symlink exists, but points to a 
	 * non-existing target. This behavior is different from the classic 
	 * {@link #exists(File) exists()}-method in the <tt>File</tt> class.
	 * 
	 * @param file A file
	 * @return Returns <tt>true</tt> if a file exists (even if its symlink target does not), <tt>false</tt> otherwise
	 */
	public static boolean exists(File file) {
		try {
			return Files.exists(Paths.get(file.getAbsolutePath()), LinkOption.NOFOLLOW_LINKS);
		}
		catch (InvalidPathException e) {
			return false;
		}
	}

	/**
	 * Replaces the {@link File#canRead() canRead()} method in the {@link File} class by taking
	 * symlinks into account. Returns <tt>true</tt> if a symlink exists even if its target file
	 * does not exist and can hence not be read.
	 * 
	 * @param file A file
	 * @return Returns <tt>true</tt> if the file can be read (or the symlink exists), <tt>false</tt> otherwise
	 */
	public static boolean canRead(File file) {
		if (isSymlink(file)) {
			return exists(file);
		}
		else {
			return file.canRead();
		}
	}
}
