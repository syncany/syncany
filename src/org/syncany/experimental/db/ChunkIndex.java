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
package org.syncany.experimental.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.syncany.util.StringUtil;

/**
 * In-memory chunk cache.
 *
 * @author pheckel
 */
public class ChunkIndex {
    private boolean loaded;
    private Map<Integer, Set<IndexEntry>> chunkCache;
    private Set<IndexEntry> metaChunkCache;

    public ChunkIndex() {
        loaded = false;
        chunkCache = new HashMap<Integer, Set<IndexEntry>>();
    }

    private synchronized void load() {
        if (loaded) {
            return;
        }

/*        EntityManager em = Config.getInstance().getDatabase().getEntityManager();

        Query q = em.createQuery("select c from CloneChunk c");
        List chunkList = q.getResultList();

        if (chunkList != null) {
            for (Object obj : chunkList) {
                add((CloneChunk) obj);
            }
        }*/

        loaded = true;
    }

    public Set<IndexEntry> get(byte[] checksum) {
        load();
        
        // Check in cache
        int checksumHash = Arrays.hashCode(checksum);
        Set<IndexEntry> cachedChunksWithHash = chunkCache.get(checksumHash);

        if (cachedChunksWithHash == null) {
            return null;
        }

        // Multiple chunks may match (b/c of meta-chunks)
        Set<IndexEntry> matchingChunks = new HashSet<IndexEntry>();

        for (IndexEntry chunk : cachedChunksWithHash) {
            if (Arrays.equals(chunk.getChecksum(), checksum)) {
                matchingChunks.add(chunk);
            }
        }

        if (matchingChunks.isEmpty()) {
            return null;
        }

        return matchingChunks;
    }

    public synchronized IndexEntry get(byte[] metaId, byte[] checksum) {
        Set<IndexEntry> checksumMatchChunks = get(checksum);

        if (checksumMatchChunks != null) {
            for (IndexEntry chunk : checksumMatchChunks) {
                if (chunk.getMetaEntry() != null && metaId != null &&
                        Arrays.equals(chunk.getMetaEntry().getChecksum(), metaId)) {

                    return chunk;
                }
            }
        }

        return null;
    }

    public synchronized void add(IndexEntry chunk) {
        load();

        int checksumHash = Arrays.hashCode(chunk.getChecksum());
        Set<IndexEntry> cachedChunksWithHash = chunkCache.get(checksumHash);

        if (cachedChunksWithHash == null) {
            cachedChunksWithHash = new HashSet<IndexEntry>();
            chunkCache.put(checksumHash, cachedChunksWithHash);
        }

        cachedChunksWithHash.add(chunk);

        /*for (Map.Entry<Integer, Set<CloneChunk>> entry : chunkCache.entrySet()) {
            for (CloneChunk cloneChunk : entry.getValue()) {
                System.out.println("chunkCache: "+entry.getKey()+"[] = "+cloneChunk);
            }
        }*/
    }
    
    public void print() {
        for (Set<IndexEntry> set : chunkCache.values()) {
            for (IndexEntry chunkEntry : set) {
                System.out.println(StringUtil.toHex(chunkEntry.getChecksum()));
            }
        }            
    }

    public void save(File file) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        
        for (Set<IndexEntry> set : chunkCache.values()) {
            for (IndexEntry indexEntry : set) {
                out.write(indexEntry.getChecksum());
                out.write(indexEntry.getMetaEntry().getChecksum());
            }
        }
        
        out.close();
    }

    public static class IndexEntry {
        private byte[] checksum;
        private IndexEntry metaEntry;

        public IndexEntry( byte[] checksum, IndexEntry metaEntry) {
            this.checksum = checksum;
            this.metaEntry = metaEntry;
        }

        public IndexEntry(byte[] checksum) {
            this.checksum = checksum;
        }

        public byte[] getChecksum() {
            return checksum;
        }

        public IndexEntry getMetaEntry() {
            return metaEntry;
        }                
    }
    
    private class ByteArrayMap {
        
    }


}
