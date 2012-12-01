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

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.syncany.config.Encryption;
import org.syncany.util.exceptions.EncryptionException;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FileUtil {
    private static final Logger logger = Logger.getLogger(FileUtil.class.getSimpleName());    
    private static final double BASE = 1024, KB = BASE, MB = KB * BASE, GB = MB * BASE;
    private static final DecimalFormat df = new DecimalFormat("#.##");

    public static String formatSize(double size) {
        if (size >= GB) {
            return df.format(size / GB) + " GiB";
        }

        if (size >= MB) {
            return df.format(size / MB) + " MiB";
        }

        if (size >= KB) {
            return df.format(size / KB) + " KiB";
        }

        return "" + (int) size + " bytes";
    }

    public static String getRelativePath(File base, File file) {
        //System.err.println("rel path = base = "+base.getAbsolutePath() + " - file: "+file.getAbsolutePath()+ " ---> ");
        if (base.getAbsolutePath().length() >= file.getAbsolutePath().length()) {
            return "";
        }

        //System.err.println("aaa"+file.getAbsolutePath().substring(base.getAbsolutePath().length() + 1));
        return file.getAbsolutePath().substring(base.getAbsolutePath().length() + 1);
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

    public static boolean renameVia(File fromFile, File toFile) {
        return renameVia(fromFile, toFile, ".ignore-rename-to-");
    }

    public static boolean renameVia(File fromFile, File toFile, String viaPrefix) {
        File tempFile = new File(toFile.getParentFile().getAbsoluteFile() + File.separator + viaPrefix + toFile.getName());
        FileUtil.deleteRecursively(tempFile); // just in case!	

        if (!fromFile.renameTo(tempFile)) {
            return false;
        }

        if (!tempFile.renameTo(toFile)) {
            tempFile.renameTo(fromFile);
            return false;
        }

        return true;
    }
    
    public static boolean deleteVia(File file) {
        return deleteVia(file, ".ignore-delete-from-");
    }
    
    public static boolean deleteVia(File file, String viaPrefix) {
        File tempFile = new File(file.getParentFile().getAbsoluteFile() + File.separator + viaPrefix + file.getName());
        FileUtil.deleteRecursively(tempFile); // just in case!	

        if (!file.renameTo(tempFile)) {
            return false;
        }

        if (!tempFile.delete()) {
            // If DELETE not successful; rename it back to the original filename
            tempFile.renameTo(file);
            return false;
        }
        
        return true;
    }        
    
    public static boolean mkdirVia(File folder) {
        return mkdirVia(folder, ".ignore-mkdir-");
    }
    
    public static boolean mkdirVia(File folder, String viaPrefix) {
        if (folder.exists()) {
            return true;
        }
        
        File canonFolder = null;
        
        try {
            canonFolder = folder.getCanonicalFile();
        } 
        catch (IOException e) {
            return false;
        }
        
        if (!canonFolder.getParentFile().exists()) {
            return false;
        }
        
        File tempFolder = new File(canonFolder.getParentFile()+File.separator+viaPrefix+canonFolder.getName());
        tempFolder.delete(); // Just in case
        
        if (!tempFolder.mkdir()) {
            return false;
        }
        
        if (!tempFolder.renameTo(canonFolder)) {
            tempFolder.delete();
            return false;
        }
        
        return true;
    }
    public static boolean mkdirsVia(File folder) {
        return mkdirsVia(folder, ".ignore-mkdirs-");
    }
    
    public static boolean mkdirsVia(File folder, String viaPrefix) {
        if (folder.exists()) {
            return true;
        }
 
        File canonFolder = null;
        
        try {
            canonFolder = folder.getCanonicalFile();
        } 
        catch (IOException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "Could not get canonical file for folder "+folder, e);
            }
            
            return false;
        }
        
        if (!canonFolder.getParentFile().exists()) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "{0} does not exist. creating.", canonFolder.getParentFile());
            }
                
            if (!mkdirsVia(canonFolder.getParentFile(), viaPrefix)) {
                return false;
            }
        }
        
        return mkdirVia(canonFolder, viaPrefix);
    }    

    public static void copy(File src, File dst) throws IOException {
        copy(new FileInputStream(src), new FileOutputStream(dst));
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        // Performance tests say 4K is the fastest (sschellh)
        byte[] buf = new byte[4096];

        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }

        in.close();
        out.close();
    }

    /**
     * Allows throttling local copy operations.
     *
     * @param src
     * @param dst
     * @param kbps
     * @throws IOException
     */
    public static void copy(File src, File dst, int kbps) throws IOException {
        if (kbps <= 0) {
            copy(src, dst);
            return;
        }

        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        int bytesPer100ms = Math.round(((float) kbps) * 1024 / 10);
        //System.out.println(new Date()+" -- bytes per 100ms:"+bytesPer100ms);
        byte[] buf = new byte[bytesPer100ms];

        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
            //System.out.println(new Date()+" -- copy "+len);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }

        //System.out.println(new Date()+" -- fertig");

        in.close();
        out.close();
    }

    public static byte[] readFile(File file) throws IOException {
        byte[] contents = new byte[(int) file.length()]; // TODO WARNING!!! load file in buffer compeltely!!

        FileInputStream fis = new FileInputStream(file);
        fis.read(contents);
        fis.close();

        return contents;
    }

    public static void writeFile(byte[] bytes, File file) throws IOException {
        writeFile(new ByteArrayInputStream(bytes), file);
    }

    public static void writeFile(InputStream is, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);

        int read = 0;
        byte[] bytes = new byte[4096];

        while ((read = is.read(bytes)) != -1) {
            fos.write(bytes, 0, read);
        }

        is.close();
        fos.close();
    }

    public static boolean deleteRecursively(File file) {
        boolean success = true;

        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                success = (f.isDirectory()) 
                    ? success && deleteRecursively(f)
                    : success && f.delete();
            }
        }

        success = success && file.delete();
        return success;
    }

    public static byte[] gzip(byte[] content) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
        gzipOutputStream.write(content);
        gzipOutputStream.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] gunzip(byte[] contentBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(new GZIPInputStream(new ByteArrayInputStream(contentBytes)), out);

        return out.toByteArray();
    }

    public static byte[] unpack(byte[] packed, Encryption enc)
            throws IOException, EncryptionException {

        byte[] decrypted = enc.decrypt(packed);
        return FileUtil.gunzip(decrypted);
    }

    public static byte[] pack(byte[] raw, Encryption enc)
            throws IOException, EncryptionException {

        byte[] gzipped = FileUtil.gzip(raw);
        return enc.encrypt(gzipped);
    }

    public static void main(String[] a) throws IOException {
        //System.out.println(getRelativeParentDirectory(new File("/home/pheckel/Coding/syncany/syncany-platop"), new File("/home/pheckel/Coding/syncany/syncany-platop/untitled folder/untitled folder")));
        //copy(new File("/home/pheckel/freed"), new File("/home/pheckel/freed2"), 100);
        System.out.println(new File("/home/pheckel").getParentFile());
    }

    public static void browsePage(String url) {
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception ex) { /* Fressen */ }
    }

    public static void openFile(final File file) {
        try {
            Desktop.getDesktop().open(file);
        } catch (Exception ex) { /* Fressen */ }
    }
    
    public static boolean checkForWriteLock(File file){
    	try {
			FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
			FileLock lock = channel.tryLock();
			if(lock == null)
				return false;
			
			lock.release();
			
			return true;
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
    }


    // showBrowseDirectoryDialog was here
    // showBrowseFileDialog was here
    // showBrowseDialogLinux was here


    // showBrowseDialogMac was here
    // showBrowseDialogDefault was here
        

}

