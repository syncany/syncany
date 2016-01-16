/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import org.syncany.database.MultiChunkEntry.MultiChunkId;

/**
 * Listener interface used by the {@link Deduper} to notify the caller of file
 * events, and to retrieve information about chunks and output files. 
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public interface DeduperListener {
	/**
	 * Called by {@link Deduper} before a file is processed. This method can be 
	 * used to ignore certain files. The method must return <tt>true</tt> if the deduper
	 * shall continue processing, or <tt>false</tt> if a file should be skipped.
	 * 
	 * <p>For files excluded by this method, neither {@link #onFileStart(File, int) onFileStart()} nor
	 * {@link #onFileEnd(File, byte[]) onFileEnd()} are called.
	 * 
	 * @param file File that is evaluated by the filter
	 * @return Returns <tt>true</tt> if the given file shall be processed, <tt>false</tt> otherwise 
	 */
	public boolean onFileFilter(File file);
	
	/**
	 * Called by {@link Deduper} before the deduplication process is started, and before the
	 * file is opened. The method must return <tt>true</tt> if the deduplication process should 
	 * continue (e.g. for regular files), and <tt>false</tt> otherwise (e.g. for directories or
	 * symlink).
	 * 
	 * <p>The method is called for every file that was not excluded by {@link #onFileFilter(File) onFileFilter()}.
	 * 
	 * @param file File for which the deduplication process is about to be started
	 * @return Returns <tt>true</tt> if the given file shall be deduplicated, <tt>false</tt> otherwise
	 */
	public boolean onFileStart(File file);
	
	/**
	 * Called by {@link Deduper} during the deduplication process for each chunk that was
	 * found in the given file.
	 * 
	 * <p>The method is called for every file that was not excluded by {@link #onFileFilter(File) onFileFilter()}.
	 * 
	 * @param file File that is being deduplicated, and for which the chunk was emitted
	 * @param chunk The new chunk that the chunker emitted
	 */
	public void onFileAddChunk(File file, Chunk chunk);
	
	/**
	 * Called by {@link Deduper} after the deduplication process of the given file, i.e. when the end of
	 * the file was reached and no more chunks can be emitted. This method also returns the checksum for the
	 * entire file content which was created during the process. 
	 * 
	 * <p>The method is called for every file that was not excluded by {@link #onFileFilter(File) onFileFilter()}.
	 * 
	 * @param file File for which the deduplication process is finished
	 * @param checksum File checksum for the entire file content (using the checksum algorithm of the chunker)
	 */
	public void onFileEnd(File file, byte[] checksum);
	
	/**
	 * Called by {@link Deduper} during the deduplication process whenever then break condition 
	 * of the {@link Chunker} was reached and a new {@link Chunk} was emitted. This method returns
	 * <tt>true</tt> if the chunk is a new (and should be further processed), and <tt>false</tt>
	 * otherwise.
	 * 
	 * <p>This method represents a query to the chunk index, i.e. it determines whether a chunk already exists
	 * in the persistence layer. If it does, the chunk should not be added to a multichunk and processing should 
	 * be stopped. Returning <tt>false</tt> has this effect. Returning <tt>true</tt> lets the deduper add the
	 * chunk to a multichunk.  
	 * 
	 * <p>The method is called zero to many times for every file, assuming that the file was not excluded
	 * by {@link #onFileFilter(File) onFileFilter()}, or by {@link #onFileStart(File, int) onFileStart()}. 
	 * 
	 * @param chunk The new chunk that the chunker emitted
	 * @return Returns <tt>true</tt> if the chunk is new, and <tt>false</tt> otherwise
	 */
	public boolean onChunk(Chunk chunk); 
	
	/**
	 * Called by {@link Deduper} during the deduplication process whenever a new {@link MultiChunk} is 
	 * created/opened. A new multichunk is opened when the previous multichunk is full.
	 * 
	 * @param multiChunk The new multichunk 
	 */
	public void onMultiChunkOpen(MultiChunk multiChunk);
	
	/**
	 * Called by {@link Deduper} during the deduplication process before a new {@link MultiChunk} is
	 * created/opened. This method must determine and return a new unique multichunk identifier.
	 *  
	 * @param firstChunk The first chunk can/might be used to determine a new multichunk identifier
	 * @return Returns a new unique multichunk identifier
	 */
	public MultiChunkId createNewMultiChunkId(Chunk firstChunk);

	/**
	 * Called by {@link Deduper} during the deduplication process before a new {@link MultiChunk} is
	 * created/opened. In order to determine the destination to which the multichunk should be written,
	 * this method returns a multichunk file to a given multichunk ID.
	 * 
	 * @param multiChunkId Identifier for the new multichunk
	 * @return Returns the (temporary or final) file to which the multichunk should be written
	 */
	public File getMultiChunkFile(MultiChunkId multiChunkId);

	/**
	 * Called by {@link Deduper} during the deduplication process whenever a new {@link Chunk} is written 
	 * to the given multichunk.  
	 * 
	 * @param multiChunk The multichunk the given chunk is being written to
	 * @param chunk The chunk that's written to the multichunk
	 */
	public void onMultiChunkWrite(MultiChunk multiChunk, Chunk chunk);
	
	/**
	 * Called by {@link Deduper} during the deduplication process whenever a multichunk is closed. This can 
	 * happen either because the multichunk is full (max. size reached/exceeded), or because there are no 
	 * more files to chunk/index.
	 *  
	 * @param multiChunk The multichunk that's being closed 
	 */
	public void onMultiChunkClose(MultiChunk multiChunk);

	/**
	 * Called by {@link Deduper} before starting the deduplication process.
	 *  
	 * @param size the number of files to be processed 
	 */
	public void onStart(int fileCount);
	
	/**
	 * Called by {@link Deduper} after finishing the deduplication process.
	 */
	public void onFinish();
}