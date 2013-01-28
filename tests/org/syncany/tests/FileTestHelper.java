package org.syncany.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

import org.syncany.chunk.chunking.Chunk;
import org.syncany.chunk.chunking.Chunker;
import org.syncany.chunk.chunking.FixedOffsetChunker;
import org.syncany.chunk.multi.CustomMultiChunk;
import org.syncany.chunk.multi.MultiChunk;
import org.syncany.config.Profile;
import org.syncany.exceptions.EncryptionException;

/**
 * 
 * This Class provides file I/O helper methods for writing tests
 * 
 * @author Nikolai Hellwig, Andreas Fenske
 *
 */
public class FileTestHelper {
	
	private static Random rnd = new Random();
	
	private static void copyFileToDirectory(File from, File toDirectory)
			throws IOException {
		// if file, then copy it
		// Use bytes stream to support all file types
		InputStream in = new FileInputStream(from);
		OutputStream out = new FileOutputStream(new File(toDirectory,
				from.getName()));

		byte[] buffer = new byte[1024];

		int length;
		// copy the file content in bytes
		while ((length = in.read(buffer)) > 0) {
			out.write(buffer, 0, length);
		}

		in.close();
		out.close();
	}

	/**
	 * 
	 * @param from Can be a folder or a file
	 * @param toDirectory Must be a folder
	 * @return 
	 * @throws IOException
	 */
	public static void copyIntoDirectory(File from, File toDirectory) throws IOException{
		if(from.isFile()){
			copyFileToDirectory(from, toDirectory);
		}else{
			if(!toDirectory.exists()){
				toDirectory.mkdir();
			}
			
			for(File f: from.listFiles()){
				File srcFile = new File(from, f.getName());
				File destFile = new File(toDirectory, f.getName());
				
				copyIntoDirectory(srcFile, destFile);
			}
		}
	}

	public static boolean emptyDirectory(File path) {
		System.out.println("emptying " + path);
		boolean ret = true;

		if(path!=null) {
			if(path.exists()) {
				for (File f : path.listFiles()) {
					boolean r = true;
					if (f.isDirectory())
						ret = deleteDirectory(f);
					else
						ret = f.delete();

					if (!r)
						ret = r;
				}
			} else {
				path.mkdirs();
			}
		}
		
		return ret;
	}

	public static boolean deleteDirectory(File path) {
		if (path!=null && path.exists() && path.isDirectory()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		} else return false;
		return (path.delete());
	}
	
	public static boolean deleteFile(File file) {
		if(file!=null && file.exists() && file.isFile()) {
			return file.delete();
		} else return false;
	}
	
	
	public static void changeRandomPartOfBinaryFile(File path, short percentage, int minSizeOfBlock) throws IOException{
		if(path!=null && !path.exists()){
			throw new IOException("File does not exist!");
		}
		
		if(percentage < 1 || percentage > 99){
			throw new IllegalArgumentException("percentage value must be between 1 and 99");
		}
		
		long fileSize = path.length();
		long maxPositions = fileSize / minSizeOfBlock;
		long percentagedSize = (long)(fileSize * (percentage / 100.0));
		long cycles = percentagedSize / minSizeOfBlock;
		
		RandomAccessFile raf = new RandomAccessFile(path, "rw");
		
		for(int i = 0; i < cycles; i++){
			long pos = (rnd.nextLong() % maxPositions) * minSizeOfBlock;
			raf.seek(pos);
			byte[] arr = createRandomArray(minSizeOfBlock);
			raf.write(arr);
		}
		
		// write last one
		byte[] arr = createRandomArray((int)(percentagedSize % minSizeOfBlock));
		long pos = (rnd.nextLong() % maxPositions) * minSizeOfBlock;
		raf.seek(pos);
		raf.write(arr);
		
		raf.close();
	}
	
	public static void generateRandomBinaryFilesIn(File rootFolder, long sizeInBytes, int numOfFiles) throws IOException{
		for(int i = 0; i <numOfFiles; i++){
			String fName = "rndFile-" + System.currentTimeMillis() + "-" + Math.abs(rnd.nextInt()) + ".dat";
			File f = new File(rootFolder, fName);
			generateRandomBinaryFile(f, sizeInBytes);
		}
	}
	
	public static void generateRandomBinaryFile(File path, long sizeInBytes) throws IOException{
		if(path!=null && path.exists()){
			throw new IOException("File already exists");
		}
		
		FileOutputStream fos = new FileOutputStream(path);
		int bufSize = 1024;
		long cycles = sizeInBytes / bufSize;
		
		for(int i = 0; i < cycles; i++){
			byte[] arr = createRandomArray(bufSize);
			fos.write(arr);
		}
		
		// create last one
		// modulo cannot exceed integer range, so cast should be ok
		byte[] arr = createRandomArray((int)(sizeInBytes % bufSize));
		fos.write(arr);
		
		fos.close();
	}
	
	public static void moveFilesIntoSubdirectory(File folder, String subDirName){
		File subDir = new File(folder, subDirName);
		subDir.mkdir();
		for(File file : folder.listFiles()){
			if(file.isFile())
				file.renameTo(new File(subDir, file.getName()));
		}
	}
	
	public static byte[] createRandomArray(int size){
		byte[] ret = new byte[size];
		rnd.nextBytes(ret);
		return ret;
	}
	
	
	public static String getMD5Checksum(File file) throws Exception {
		if(file==null) throw new Exception("File is null!");
		
		byte[] b = createChecksum(file);
		String result = "";

		for (int i=0; i < b.length; i++) {
			result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
		}
		return result;
	}
	
	public static byte[] createChecksum(File filename) throws Exception {
		FileInputStream fis =  new FileInputStream(filename);

		byte[] buffer = new byte[1024];
		MessageDigest complete = MessageDigest.getInstance("MD5");
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
	
	public static ArrayList<MultiChunk> getMultiChunksOfFile(File file) throws IOException, EncryptionException {
		return FileTestHelper.getMultiChunksOfFile(file, null);
	}
	
	public static ArrayList<MultiChunk> getMultiChunksOfFile(File file, File path) throws IOException, EncryptionException {
//		Chunker chunker = Profile.getInstance().getRepository().getChunker();
		
		if(file==null || !file.exists()) throw new FileNotFoundException();
		
		if(path==null) path = TestSettings.getInstance().getAppCacheDir();
		else if(!path.exists() && path.isDirectory()) path.mkdirs();
		else throw new FileNotFoundException();
		
		Chunker chunker = new FixedOffsetChunker(16*1024);
		Enumeration<Chunk> chunkEnum = chunker.createChunks(file);
		
//		List<String> chunkIdStrs = new ArrayList<String>();
		
		Chunk chunk = null;
		MultiChunk metaChunk = null;
		File metaChunkTempFile = null;
		
		ArrayList<MultiChunk> generatedMetaChunks = new ArrayList<MultiChunk>();
		
		while (chunkEnum.hasMoreElements()) {
			chunk = chunkEnum.nextElement();   
			
            if (metaChunk != null && metaChunk.isFull()) {
            	generatedMetaChunks.add(metaChunk);
            	metaChunk = closeMultiChunk(metaChunk, metaChunkTempFile);
            }
            
            byte[] metaId;
            
            if (metaChunk != null && metaChunk.getId() != null) {
                metaId = metaChunk.getId();
            }
            else {
                metaId = chunk.getChecksum(); 
            }
            
            if (metaChunk == null) {
            	int chunkSize = Profile.getInstance().getRepository().getChunkSize()*1024;
            	Cipher encCipher = Profile.getInstance().getRepository().getEncryption().createEncCipher(metaId);
            	metaChunkTempFile = createTempFile("multichunk", path);
            	metaChunk = new CustomMultiChunk(metaId, chunkSize, new CipherOutputStream(new FileOutputStream(metaChunkTempFile), encCipher));
            }
            
            metaChunk.write(chunk);
		}
		if(metaChunk!=null) {
			generatedMetaChunks.add(metaChunk);
			closeMultiChunk(metaChunk, metaChunkTempFile);
		}
		return generatedMetaChunks;
	}
	
	private static MultiChunk closeMultiChunk(MultiChunk multiChunk, File multiChunkTempFile) throws IOException {
		multiChunk.close();
		File multiChunkFile = Profile.getInstance().getCache().getMetaChunkFile(multiChunk);
		multiChunkTempFile.renameTo(multiChunkFile);
		
		multiChunk = null;
		return multiChunk;
	}
	
	private static File createTempFile(String name, File folder) throws IOException {
		return File.createTempFile(String.format("temp-%s-", name),".tmp", folder);
	}
}
