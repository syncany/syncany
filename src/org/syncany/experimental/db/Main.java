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
import java.io.IOException;
import java.util.LinkedList;

/**
 *
 * @author pheckel
 */
public class Main {
    public static void main(String[] args) throws IOException {
        File dbFile = new File("/home/pheckel/Syncany/TEST/memdb");
        File dbFile2 = new File("/home/pheckel/Syncany/TEST/memdb2");
        
        Database db = new Database();
        
        if (dbFile.exists()) {
            db.load(dbFile);
            db.load(dbFile2);  
             for (FileHistory fileHistory : db.getFileHistories()) {
                System.out.println(fileHistory.getFileId());
                //System.out.println(fileHistory.getLastVersion().getName());
                
                 for (FileVersion fileVersion : fileHistory.getVersions()) {
                     System.out.println(fileVersion.getName());
                 }
             }
            
            
            
            if (true)  System.exit(0);
            
            
            
            Content newc = db.createContent();
            
            newc.addChunk(db.getChunk(new byte[] { 1,2,3,4,5,7,8,9,0}));
            newc.addChunk(db.getChunk(new byte[] { 1,2,3,4,5,7,8,9,0}));
            newc.addChunk(db.getChunk(new byte[] { 1,2,3,4,5,7,8,9,0}));
            newc.setChecksum(new byte[]{65,65,65,65,65,65,65,65,65});
            
            db.addContent(newc);
            
            
            for (FileHistory fileHistory : db.getFileHistories()) {
                System.out.println(fileHistory.getFileId());
                System.out.println(fileHistory.getLastVersion().getName());
                FileVersion newVersion = db.createFileVersion(fileHistory);
                newVersion.setStatus(FileVersion.Status.RENAMED);
                newVersion.setName("LLLLLLLLLLLOOOOOOOOOONNNNNNNNNNNNNGGGGGGGG");                
            }
            
            db.save(dbFile2, false);
            //db.dump();
            
            System.exit(0);
        }
        
        // CHECKSUMS MUST BE OF SAME LENGTH!!
    
        ChunkEntry a = db.createChunk(new byte[] { 1,2,3,4,5,7,8,9,0}, 12,true);
        ChunkEntry b = db.createChunk(new byte[] { 9,8,7,6,5,4,3,2,1}, 34, true);
        ChunkEntry c = db.createChunk(new byte[] { 1,1,1,1,1,1,1,1,1}, 56, true);
        ChunkEntry d = db.createChunk(new byte[] { 2,2,2,2,2,2,2,2,2}, 78, true);
        ChunkEntry e = db.createChunk(new byte[] { 3,3,3,3,3,3,3,3,3}, 910, true);
        ChunkEntry f = db.createChunk(new byte[] { 4,4,4,4,4,4,4,4,4}, 1112, true);
         
        Content c1 = db.createContent();
        c1.addChunk(a);
        c1.addChunk(a);
        c1.addChunk(b);
        c1.setChecksum(new byte[]{5,5,5,5,5,5,5,5,5});      
        
        FileHistory file1 = db.createFileHistory(true);
        
        FileVersion v1 = db.createFileVersion(file1);
        v1.setPath("file/path");
        v1.setName("file.jpg");
        v1.setContent(c1);
        
        FileVersion v2 = db.createFileVersion(file1);
        v2.setName("renamed");        
        // copied from v1
        
        MetaChunkEntry m1 = db.createMetaChunk();
        m1.addChunk(a);
        m1.addChunk(b);
        m1.addChunk(c);
        m1.addChunk(d);
        m1.setChecksum(new byte[] {6,6,6,6,6,6,6,6,6});
        
        MetaChunkEntry m2 = db.createMetaChunk();
        m2.addChunk(a);
        m2.addChunk(b);
        m2.addChunk(c);
        m2.addChunk(d);
        m2.setChecksum(new byte[] {7,7,7,7,7,7,7,7,7});


        db.addContent(c1);
        db.addMetaChunk(m1);
        db.addMetaChunk(m2);
        
        db.save(dbFile);
        
        
        
        if (true) System.exit(0);
        
        /*
        ChunkCache index = new ChunkCache(em);
        index.load();
        
        Chunk chunk = index.getChunk(new byte[]{8,8,8,8,8,8,8,8,8}, true);
        System.out.println("NEW "+chunk);       
        index.persist();
        index.save(new File("/home/pheckel/Syncany/TEST/INDEX.zip"));
        //index.list();
        
        
        // CHANGE and repack
        
        Content c2 = new Content();
        c2.addChunk(a);
        c2.addChunk(d); // << changed
        c2.addChunk(b);
        c2.setChecksum(new byte[]{9,9,9,9,9,9,9,9,9});                
        
        FileVersion v3 = file1.createVersion();
        v3.setContent(c2);
        
        em.getTransaction().begin();
        em.persist(c2);
        em.persist(v3);
        em.getTransaction().commit();
        
        index.save(new File("/home/pheckel/Syncany/TEST/INDEX2.zip"));
        
        /*
        Map<Integer, Set<MetaChunk>> metaChunkCache = new HashMap<Integer, Set<MetaChunk>>();
        Query q = em.createQuery("select mc from MetaChunk mc");
        List metaChunkList = q.getResultList();
        if (metaChunkList != null) {
        for (Object obj : metaChunkList) {
        MetaChunk metaChunk = (MetaChunk) obj;
        int metaChunkHashCode = Arrays.hashCode(metaChunk.getChecksum());
        Set<MetaChunk> metaChunksWithHashCode = metaChunkCache.get(metaChunkHashCode);
        if (metaChunksWithHashCode == null) {
        metaChunksWithHashCode = new HashSet<MetaChunk>();
        metaChunkCache.put(metaChunkHashCode, metaChunksWithHashCode);
        }
        System.out.println("m "+metaChunk);
        metaChunksWithHashCode.add(metaChunk);
        }
        }
        Map<Integer, Set<Chunk>> chunkCache = new HashMap<Integer, Set<Chunk>>();
        q = em.createQuery("select c from Chunk c");
        List chunkList = q.getResultList();
        if (chunkList != null) {
        for (Object obj : chunkList) {
        Chunk chunk = (Chunk) obj;
        int chunkHashCode = Arrays.hashCode(chunk.getChecksum());
        Set<Chunk> chunksWithHashCode = chunkCache.get(chunkHashCode);
        if (chunksWithHashCode == null) {
        chunksWithHashCode = new HashSet<Chunk>();
        chunkCache.put(chunkHashCode, chunksWithHashCode);
        }
        System.out.println("c "+chunk);
        chunksWithHashCode.add(chunk);
        }
        }        */
       
    }        
}
