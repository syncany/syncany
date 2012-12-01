/*
 * Syncany
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com>
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
package org.syncany.db;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.syncany.config.Settings;
import org.syncany.util.AppendableObjectOutputStream;

/**
 * In-memory chunk cache.
 * 
 * @author pheckel
 */
public class ChunkCache {
	private Map<Integer, Set<CloneChunk>> chunkCache;

	public ChunkCache() {
		chunkCache = new HashMap<Integer, Set<CloneChunk>>();
		load();
	}

	// TODO: Implement this
	private synchronized void load() {
		try {
			File f = new File(Settings.getInstance().getChunkDbFile());
			if (!f.exists()) {
				return;
			}

			FileInputStream fis = new FileInputStream(Settings.getInstance()
					.getChunkDbFile());

			ObjectInputStream ois = new ObjectInputStream(fis);
			Object obj = null;

			while ((obj = ois.readObject()) != null) {
				if (obj instanceof CloneChunk) {
					add((CloneChunk) obj, false);
				}
			}

			ois.close();
		} catch (EOFException ex) {
			return; // Empty File
		} catch (FileNotFoundException e) {
			// TODO: Implement proper daemon error handling
			e.printStackTrace();
		} catch (IOException e) {
			// TODO: Implement proper daemon error handling
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO: Implement proper daemon error handling
			e.printStackTrace();
		}
	}

	public Set<CloneChunk> get(byte[] checksum) {
		// Check in cache
		int checksumHash = Arrays.hashCode(checksum);
		Set<CloneChunk> cachedChunksWithHash = chunkCache.get(checksumHash);

		if (cachedChunksWithHash == null) {
			return null;
		}

		// Multiple chunks may match (b/c of meta-chunks)
		Set<CloneChunk> matchingChunks = new HashSet<CloneChunk>();

		for (CloneChunk chunk : cachedChunksWithHash) {
			if (Arrays.equals(chunk.getChecksum(), checksum)) {
				matchingChunks.add(chunk);
			}
		}

		if (matchingChunks.isEmpty()) {
			return null;
		}

		return matchingChunks;
	}

	public synchronized CloneChunk get(byte[] metaId, byte[] checksum) {
		Set<CloneChunk> checksumMatchChunks = get(checksum);

		if (checksumMatchChunks != null) {
			for (CloneChunk chunk : checksumMatchChunks) {
				if (chunk.getMetaId() != null && metaId != null
						&& Arrays.equals(chunk.getMetaId(), metaId)) {

					return chunk;
				}
			}
		}

		return null;
	}

	public static synchronized void initFile() throws IOException {
		File f = new File(Settings.getInstance().getChunkDbFile());

		f.delete();
		f.createNewFile();
	}

	public synchronized void add(CloneChunk chunk) {
		add(chunk, true);
	}

	public synchronized void add(CloneChunk chunk, boolean writeToFile) {
		int checksumHash = Arrays.hashCode(chunk.getChecksum());
		Set<CloneChunk> cachedChunksWithHash = chunkCache.get(checksumHash);

		if (cachedChunksWithHash == null) {
			cachedChunksWithHash = new HashSet<CloneChunk>();
			chunkCache.put(checksumHash, cachedChunksWithHash);
		}

		cachedChunksWithHash.add(chunk);

		if (!writeToFile)
			return;

		FileOutputStream fos = null;
		ObjectOutputStream oos = null;

		boolean append = testValidFile();
		try {
			fos = new FileOutputStream(Settings.getInstance().getChunkDbFile(),
					append); // append
								// to
								// end
								// of
								// file
			if (append)
				oos = new AppendableObjectOutputStream(fos);
			else
				oos = new ObjectOutputStream(fos);

			oos.writeObject(chunk);
			oos.close();
		} catch (FileNotFoundException e) {
			// TODO: Implement proper daemon error handling
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			// try closing oos to avoid write-locks
			if (oos != null) {
				try {
					oos.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private boolean testValidFile() {
		try {
			FileInputStream fis = new FileInputStream(Settings.getInstance()
					.getChunkDbFile());

			ObjectInputStream ois = new ObjectInputStream(fis);
			ois.close();
		} catch (IOException ioex) {
			return false;
		}

		return true;
	}

	public int getSize() {
		return chunkCache.size();
	}
	
	public void cleanChunkCache() {
		chunkCache.clear();
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		for (Entry<Integer, Set<CloneChunk>> entry : chunkCache.entrySet()) {
			boolean first = true;
			for (CloneChunk c : entry.getValue()) {
				if (first) {
					sb.append("Checksum: " + c.getChecksum() + "\r\n");
					first = false;
				}
				sb.append("\t Chunk: " + c.getIdStr() + "\r\n");
			}
			sb.append("\r\n");
		}

		return sb.toString();
	}

}
