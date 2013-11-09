package org.syncany.chunk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

/**
 * The Deduper implements the core deduplication algorithm used by Syncany. 
 * 
 * <p>The algorithm uses a {@link Chunker} to break files into individual
 * {@link Chunk}s. These chunks are added to a {@link MultiChunk} using an implementation
 * of a {@link MultiChunker}. Before this multichunk is written to a file, it is transformed
 * using one or many {@link Transformer}s (can be chained). 
 * 
 * <p>This class does not maintain a chunk index itself. Instead, it calls a listener to
 * lookup a chunk, and skips further chunk processing if the chunk already exists. 
 * 
 * <p>For a detailed description of the algorithm, please refer to chapter 5.3 of the thesis:
 * <i>"Minimizing remote storage usage and synchronization time using deduplication and
 * multichunking: Syncany as an example"</i>
 * 
 * @see <a href="http://blog.philippheckel.com/2013/05/20/minimizing-remote-storage-usage-and-synchronization-time-using-deduplication-and-multichunking-syncany-as-an-example/">Blog post: Minimizing remote storage usage and synchronization time using deduplication and multichunking: Syncany as an example</a>
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Deduper {	
	private Chunker chunker;
	private MultiChunker multiChunker;
	private Transformer transformer;

	public Deduper(Chunker chunker, MultiChunker multiChunker, Transformer transformer) {		
		this.chunker = chunker;
		this.multiChunker = multiChunker;
		this.transformer = transformer;
	}
	
	/**
	 * Deduplicates the given list of files according to the Syncany chunk algorithm. 
	 * 
	 * <p>A brief description of the algorithm (and further links to a detailed description)
	 * are given in the {@link Deduper}.
	 *  	
	 * @param files List of files to be deduplicated
	 * @param listener Listener to react of file/chunk/multichunk events, and to implement the chunk index
	 * @throws IOException If a file cannot be read or an unexpected exception occurs
	 */
	public void deduplicate(List<File> files, DeduperListener listener) throws IOException {
		Chunk chunk = null;
		MultiChunk multiChunk = null;
		
		for (File file : files) {
			// Filter ignored files
			boolean fileAccepted = listener.onFileStart(file);
			
			if (!fileAccepted) {
				continue;
			}
			
			// Decide whether to index the contents
			boolean dedupContents = listener.onFileStartDeduplicate(file);

			if (dedupContents) {
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

						// - Open new multichunk if non-existent
						if (multiChunk == null) {
							byte[] newMultiChunkId = listener.createNewMultiChunkId(chunk);
							File multiChunkFile = listener.getMultiChunkFile(newMultiChunkId);
							
							multiChunk = multiChunker.createMultiChunk(newMultiChunkId, 
								transformer.createOutputStream(new FileOutputStream(multiChunkFile)));

							listener.onOpenMultiChunk(multiChunk);
						}

						// - Add chunk data
						multiChunk.write(chunk);						
						listener.onWriteMultiChunk(multiChunk, chunk);						
					}

					listener.onFileAddChunk(file, chunk);										
				}

				// Closing file is necessary!
				chunker.close(); 
			}

			if (chunk != null) {			
				listener.onFileEnd(file, chunk.getFileChecksum());
			}
			else {
				listener.onFileEnd(file, null);
			}
			
			// Reset chunk (if folder after chunk, the folder would have a checksum b/c of chunk.getFileChecksum())
			chunk = null;
		}

		// Close and add last multichunk
		if (multiChunk != null) {
			// Data
			multiChunk.close();
			listener.onCloseMultiChunk(multiChunk);

			multiChunk = null;
		}		
	}	
}
