/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FileUtil {
    public static String getRelativePath(File base, File file) {
        //System.err.println("rel path = base = "+base.getAbsolutePath() + " - file: "+file.getAbsolutePath()+ " ---> ");
        if (base.getAbsolutePath().length() >= file.getAbsolutePath().length()) {
            return "";
        }

        String relativeFilePath = file.getAbsolutePath().substring(base.getAbsolutePath().length() + 1);
        
        // Remove trailing slashes
        while (relativeFilePath.endsWith(File.separator)) {
        	relativeFilePath = relativeFilePath.substring(0, relativeFilePath.length()-1);
        }

        // Remove leading slashes
        while (relativeFilePath.startsWith(File.separator)) {
        	relativeFilePath = relativeFilePath.substring(1);
        }
        
        return relativeFilePath;
    }

    public static String getAbsoluteParentDirectory(File file) {
        return file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(File.separator));
    }

    public static String getAbsoluteParentDirectory(String absFilePath) {
        return absFilePath.substring(0, absFilePath.lastIndexOf(File.separator));
    }

    public static String getRelativeParentDirectory(File base, File file) {
        //System.out.println(new File(getAbsoluteParentDirectory(file)));
        //System.err.println("reldir -> base = "+base.getAbsolutePath() + " - file: "+file.getAbsolutePath()+" ---> "+getRelativePath(base, new File(getAbsoluteParentDirectory(file))));
        return getRelativePath(base, new File(getAbsoluteParentDirectory(file)));
    }

    public static List<File> getRecursiveFileList(File root) throws FileNotFoundException {
        return getRecursiveFileList(root, false);
    }

    public static List<File> getRecursiveFileList(File root, boolean includeDirectories) throws FileNotFoundException {
        if (!root.isDirectory() || !root.canRead() || !root.exists()) {
            throw new FileNotFoundException("Invalid directory " + root);
        }

        List<File> result = getRecursiveFileListNoSort(root, includeDirectories);
        Collections.sort(result);

        return result;
    }

    private static List<File> getRecursiveFileListNoSort(File root, boolean includeDirectories) {
        List<File> result = new ArrayList<File>();
        List<File> filesDirs = Arrays.asList(root.listFiles());

        for (File file : filesDirs) {
            if (!file.isDirectory() || includeDirectories) {
                result.add(file);
            }

            if (file.isDirectory()) {
                List<File> deeperList = getRecursiveFileListNoSort(file, includeDirectories);
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

        return ((includeDot) ? "." : "")
                + filename.substring(dot + 1, filename.length());
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
        } catch (IOException ex) {
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

	public static byte[] createChecksum(File file) throws Exception {
		return createChecksum(file, "SHA1");
	}
	
	public static byte[] createChecksum(File filename, String digestAlgorithm) throws Exception {
		FileInputStream fis =  new FileInputStream(filename);

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
			// Test 1: Missing permissions or locked file parts
			randomAccessFile = new RandomAccessFile(file, "rw");
			
			// Test 2: Set lock and release it again
			FileLock fileLock = randomAccessFile.getChannel().tryLock();
			
			if (fileLock == null) {
				fileLocked = true;
			}
			else {
				try { fileLock.release(); }
				catch (Exception e) { /* Nothing */ }
			}
		}
		catch (Exception e) {
		    fileLocked = true;
		}
		finally {				
		    if (randomAccessFile != null) {
				try { randomAccessFile.close(); }
				catch (IOException e) { /* Nothing */ }
		    }
		}
			
		return fileLocked;
	}

	public static String getName(String fullName) {
		return new File(fullName).getName();
	}
	
	public static boolean symlinksSupported() {
		return File.separatorChar == '/';
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
	
	public static void createSymlink(File targetFile, File symlinkFile) throws Exception {
		Path targetPath = Paths.get(targetFile.getAbsolutePath());
		Path symlinkPath = Paths.get(symlinkFile.getAbsolutePath());
		
		Files.createSymbolicLink(symlinkPath, targetPath);
	}
	
}

