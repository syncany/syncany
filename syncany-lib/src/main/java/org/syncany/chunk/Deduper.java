/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.chunk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.syncany.chunk.Chunker.ChunkEnumeration;
import org.syncany.database.MultiChunkEntry.MultiChunkId;

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
	private long maxTotalSize;
	private long maxNumberOfFiles;

	public Deduper(Chunker chunker, MultiChunker multiChunker, Transformer transformer, long maxTotalSize, long maxNumberOfFiles) {
		this.chunker = chunker;
		this.multiChunker = multiChunker;
		this.transformer = transformer;
		this.maxTotalSize = maxTotalSize;
		this.maxNumberOfFiles = maxNumberOfFiles;
	}
	
	/**
	 * Deduplicates the given list of files according to the Syncany chunk algorithm. 
	 * 
	 * <p>A brief description of the algorithm (and further links to a detailed description)
	 * are given in the {@link Deduper}.
	 *  	
	 * @param files List of files to be deduplicated (will be modified!)
	 * @param listener Listener to react of file/chunk/multichunk events, and to implement the chunk index
	 * @throws IOException If a file cannot be read or an unexpected exception occurs
	 */
	public void deduplicate(List<File> files, DeduperListener listener) throws IOException {
		Chunk chunk = null;
		MultiChunk multiChunk = null;
		long totalMultiChunkSize = 0L;
		long totalNumFiles = 0L;
		
		while (!files.isEmpty()) {
			File file = files.remove(0);
			totalNumFiles++;
			
			// Filter ignored files
			boolean fileAccepted = listener.onFileFilter(file);
			
			if (!fileAccepted) {
				continue;
			}
			
			// Decide whether to index the contents
			boolean dedupContents = listener.onFileStart(file);

			if (dedupContents) {
				// Create chunks from file
				ChunkEnumeration chunksEnum = chunker.createChunks(file);

				while (chunksEnum.hasMoreElements()) {
					chunk = chunksEnum.nextElement();

					// old chunk
					if (!listener.onChunk(chunk)) {
						listener.onFileAddChunk(file, chunk);
						continue;
					}

					// new chunk
					else {					
						// - Check if multichunk full
						if (multiChunk != null && multiChunk.isFull()) {
							totalMultiChunkSize += multiChunk.getSize();
							multiChunk.close();
							listener.onMultiChunkClose(multiChunk);

							multiChunk = null;
						}

						// - Open new multichunk if non-existent
						if (multiChunk == null) {
							MultiChunkId newMultiChunkId = listener.createNewMultiChunkId(chunk);
							File multiChunkFile = listener.getMultiChunkFile(newMultiChunkId);
							
							multiChunk = multiChunker.createMultiChunk(newMultiChunkId, 
								transformer.createOutputStream(new FileOutputStream(multiChunkFile)));

							listener.onMultiChunkOpen(multiChunk);
						}

						// - Add chunk data
						multiChunk.write(chunk);						
						listener.onMultiChunkWrite(multiChunk, chunk);						
					}

					listener.onFileAddChunk(file, chunk);										
				}

				// Closing file is necessary!
				chunksEnum.close();

			}

			if (chunk != null) {			
				listener.onFileEnd(file, chunk.getFileChecksum());
			}
			else {
				listener.onFileEnd(file, null);
			}
			
			// Reset chunk (if folder after chunk, the folder would have a checksum b/c of chunk.getFileChecksum())
			chunk = null;

			// Check if we have reached the transaction limit
			if (multiChunk != null) {
				if (totalMultiChunkSize + multiChunk.getSize() >= maxTotalSize || totalNumFiles >= maxNumberOfFiles) {
					multiChunk.close();
					listener.onMultiChunkClose(multiChunk);
					return;
				}
			}
			else if (totalMultiChunkSize >= maxTotalSize || totalNumFiles >= maxNumberOfFiles) {
				return;
			}
		}

		// Close and add last multichunk
		if (multiChunk != null) {
			// Data
			multiChunk.close();
			listener.onMultiChunkClose(multiChunk);

			multiChunk = null;
		}
		
		listener.onFinish();

		return;
	}	
}
