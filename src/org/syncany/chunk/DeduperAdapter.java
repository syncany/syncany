package org.syncany.chunk;

import java.io.File;

import org.syncany.util.FileUtil;

/**
 * Implements a {@link DeduperListener} with empty methods. Can be used to
 * selectively override methods. 
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class DeduperAdapter implements DeduperListener {
	public boolean onFileStart(File file) { return true; } 
	public boolean onFileStartDeduplicate(File file) { return file.isFile() && !FileUtil.isSymlink(file); }
	public void onFileAddChunk(File file, Chunk chunk) { }
	public void onFileEnd(File file, byte[] checksum) { }
		
	public void onOpenMultiChunk(MultiChunk multiChunk) { }
	public void onWriteMultiChunk(MultiChunk multiChunk, Chunk chunk) { }
	public void onCloseMultiChunk(MultiChunk multiChunk) { }

	public abstract boolean onChunk(Chunk chunk);
	public abstract File getMultiChunkFile(byte[] multiChunkId);
}