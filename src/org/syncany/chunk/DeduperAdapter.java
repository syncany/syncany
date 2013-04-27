package org.syncany.chunk;

import java.io.File;

public abstract class DeduperAdapter implements DeduperListener {
	public void onFinish() { }
	public void onStart() { }
	
	public void onFileStart(File file) { } 
	public void onFileAddChunk(File file, Chunk chunk) { }
	public void onFileEnd(File file, byte[] checksum) { }
		
	public void onOpenMultiChunk(MultiChunk multiChunk) { }
	public void onWriteMultiChunk(MultiChunk multiChunk, Chunk chunk) { }
	public void onCloseMultiChunk(MultiChunk multiChunk) { }

	public abstract boolean onChunk(Chunk chunk);
	public abstract File getMultiChunkFile(byte[] multiChunkId);
}