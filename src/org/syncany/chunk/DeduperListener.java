package org.syncany.chunk;

import java.io.File;

/**
 * Listener interface used by the {@link Deduper} to notify the caller of file
 * events, and to retrieve information about chunks and output files. 
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public interface DeduperListener {
	public boolean onFileBeforeStart(File file);
	public boolean onFileStart(File file);
	public void onFileAddChunk(File file, Chunk chunk);
	public void onFileEnd(File file, byte[] checksum);
	
	public boolean onChunk(Chunk chunk); // return TRUE if new, FALSE if old
	
	public void onOpenMultiChunk(MultiChunk multiChunk);
	public void onWriteMultiChunk(MultiChunk multiChunk, Chunk chunk);
	public void onCloseMultiChunk(MultiChunk multiChunk);		
	public File getMultiChunkFile(byte[] multiChunkId);
	public byte[] createNewMultiChunkId(Chunk firstChunk);
}