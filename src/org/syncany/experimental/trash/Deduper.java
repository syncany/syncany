package org.syncany.experimental.trash;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

import org.syncany.chunk.chunking.Chunk;
import org.syncany.chunk.chunking.Chunker;
import org.syncany.chunk.multi.MultiChunk;
import org.syncany.chunk.multi.MultiChunker;
import org.syncany.chunk.transform.Transformer;

public class Deduper {
	public static interface IndexerListener {
		public void onFileStart(File file);
		public void onFileAddChunk(Chunk chunk);
		public void onFileEnd(byte[] checksum);
		
		public boolean onChunk(Chunk chunk); // return TRUE if new, FALSE if old
		
		public void onOpenMultiChunk(MultiChunk multiChunk);
		public void onWriteMultiChunk(MultiChunk multiChunk, Chunk chunk);
		public void onCloseMultiChunk(MultiChunk multiChunk);		
		public File getMultiChunkFile(byte[] multiChunkId);
	} 	
	
	public void deduplicate(List<File> files, Chunker chunker, MultiChunker multiChunker, Transformer transformer, IndexerListener listener) throws IOException {
		Chunk chunk = null;
		MultiChunk multiChunk = null;

		for (File file : files) {
			listener.onFileStart(file);

			if (file.isFile()) {
				// Create chunks from file
				Enumeration<Chunk> chunks = chunker.createChunks(file);

				while (chunks.hasMoreElements()) {
					chunk = chunks.nextElement();

					// old chunk
					if (!listener.onChunk(chunk)) {
						continue;
					}

					// new chunk
					else {					
						// - Check if multichunk full
						if (multiChunk != null && multiChunk.isFull()) {
							multiChunk.close();
							multiChunk = null;

							listener.onCloseMultiChunk(multiChunk);
						}

						// - Open new multichunk if none existant
						if (multiChunk == null) {
							File multiChunkFile = listener.getMultiChunkFile(chunk.getChecksum());
							multiChunk = multiChunker.createMultiChunk(chunk.getChecksum(), 
								transformer.transform(new FileOutputStream(multiChunkFile)));

							listener.onOpenMultiChunk(multiChunk);

						}

						// - Add chunk data
						multiChunk.write(chunk);						
						listener.onWriteMultiChunk(multiChunk, chunk);						
					}

					listener.onFileAddChunk(chunk);					
					
				}

			}

			if (chunk != null) {			
				listener.onFileEnd(chunk.getChecksum());
			}
			else {
				listener.onFileEnd(null);
			}
		}

		// Close and add last multichunk
		if (multiChunk != null) {
			// Data
			multiChunk.close();
			multiChunk = null;

			listener.onCloseMultiChunk(multiChunk);
		}

	}


}
