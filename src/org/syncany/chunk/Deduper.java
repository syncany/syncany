package org.syncany.chunk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;


public class Deduper {	
	private Chunker chunker;
	private MultiChunker multiChunker;
	private Transformer transformer;

	public Deduper(Chunker chunker, MultiChunker multiChunker, Transformer transformerChain) {		
		this.chunker = chunker;
		this.multiChunker = multiChunker;
		this.transformer = transformerChain;
	}
	
	public void deduplicate(List<File> files, DeduperListener listener) throws IOException {
		Chunk chunk = null;
		MultiChunk multiChunk = null;
		
		listener.onStart();
		
		for (File file : files) {
			listener.onFileStart(file);

			if (file.isFile()) {
				// Create chunks from file
				Enumeration<Chunk> chunks = chunker.createChunks(file);

				while (chunks.hasMoreElements()) {
					chunk = chunks.nextElement();

					// old chunk
					if (!listener.onChunk(chunk)) {
						listener.onFileAddChunk(file, chunk);
						continue;
					}

					// new chunk
					else {					
						// - Check if multichunk full
						if (multiChunk != null && multiChunk.isFull()) {
							multiChunk.close();
							listener.onCloseMultiChunk(multiChunk);

							multiChunk = null;
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

					listener.onFileAddChunk(file, chunk);					
					
				}

			}

			if (chunk != null) {			
				listener.onFileEnd(file, chunk.getChecksum());
			}
			else {
				listener.onFileEnd(file, null);
			}
		}

		// Close and add last multichunk
		if (multiChunk != null) {
			// Data
			multiChunk.close();
			listener.onCloseMultiChunk(multiChunk);

			multiChunk = null;
		}
		
		listener.onFinish();
	}	
}
