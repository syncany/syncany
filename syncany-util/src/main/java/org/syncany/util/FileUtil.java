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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

/**
 * A file utility class
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FileUtil {
	public static String getRelativePath(File base, File file) {
		return removeTrailingSlash(base.toURI().relativize(file.toURI()).getPath());
	}

	public static String getRelativeDatabasePath(File base, File file) {
		String relativeFilePath = getRelativePath(base, file);
		return getDatabasePath(relativeFilePath);
	}

	public static String getDatabasePath(String filePath) {
		// Note: This is more important than it seems. Unix paths may contain backslashes
		// so that 'black\white.jpg' is a perfectly valid file path. Windows file names
		// may never contain backslashes, so that '\' can be safely transformed to the
		// '/'-separated database path!

		if (EnvironmentUtil.isWindows()) {
			return filePath.toString().replaceAll("\\\\", "/");
		}
		else {
			return filePath;
		}
	}

	public static String removeTrailingSlash(String filename) {
		if (filename.endsWith("/")) {
			return filename.substring(0, filename.length() - 1);
		}
		else {
			return filename;
		}
	}

	public static File getCanonicalFile(File file) {
		try {
			return file.getCanonicalFile();
		}
		catch (IOException ex) {
			return file;
		}
	}

	public static byte[] createChecksum(File filename, String digestAlgorithm) throws NoSuchAlgorithmException, IOException {
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

	public static String dosAttrsToString(DosFileAttributes dosFileAttributes) {
		return LimitedDosFileAttributes.toString(dosFileAttributes);
	}

	public static LimitedDosFileAttributes dosAttrsFromString(String dosFileAttributes) {
		return new LimitedDosFileAttributes(dosFileAttributes);
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

	public static boolean isDirectory(File file) {
		try {
			return Files.isDirectory(Paths.get(file.getAbsolutePath()), LinkOption.NOFOLLOW_LINKS);
		}
		catch (InvalidPathException e) {
			return false;
		}
	}

	public static String formatFileSize(long size) {
		if (size <= 0) {
			return "0";
		}

		final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}
}
