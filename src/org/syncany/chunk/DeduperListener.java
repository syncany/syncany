package org.syncany.chunk;

import java.io.File;

public interface DeduperListener {
	public void onFinish();
	public void onStart();
	
	public void onFileStart(File file);
	public void onFileAddChunk(File file, Chunk chunk);
	public void onFileEnd(File file, byte[] checksum);
	
	public boolean onChunk(Chunk chunk); // return TRUE if new, FALSE if old
	
	public void onOpenMultiChunk(MultiChunk multiChunk);
	public void onWriteMultiChunk(MultiChunk multiChunk, Chunk chunk);
	public void onCloseMultiChunk(MultiChunk multiChunk);		
	public File getMultiChunkFile(byte[] multiChunkId);
}