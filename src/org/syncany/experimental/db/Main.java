package org.syncany.experimental.db;

import java.io.File;
import java.io.IOException;

public class Main {

	
	public static void main(String[] args) throws IOException {
		Database db;
		
		save1();
		db = load1();
		
		save2(db);
	}
	
	public static Database save1() throws IOException {
		Database db = new Database();
		
		// Create first file
        ChunkEntry chunkA1 = db.createChunk(new byte[] { 1,2,3,4,5,7,8,9,0}, 12,true);
        ChunkEntry chunkA2 = db.createChunk(new byte[] { 9,8,7,6,5,4,3,2,1}, 34, true);
        ChunkEntry chunkA3 = db.createChunk(new byte[] { 1,1,1,1,1,1,1,1,1}, 56, true);
        ChunkEntry chunkA4 = db.createChunk(new byte[] { 2,2,2,2,2,2,2,2,2}, 78, true);
		
        Content contentA = db.createContent();        
        contentA.addChunk(chunkA1);
        contentA.addChunk(chunkA2);
        contentA.addChunk(chunkA3);
        contentA.addChunk(chunkA4);
        contentA.setChecksum(new byte[]{5,5,5,4,4,5,5,5,5});              
        db.addContent(contentA);
        
        FileHistory fileA = db.createFileHistory(true);
        
        FileVersion versionA1 = db.createFileVersion(fileA);
        versionA1.setPath("some/path");
        versionA1.setName("fileA.jpg");
        versionA1.setContent(contentA);
       // fileA.addVersion(versionA1);
        
        FileVersion versionA2 = db.createFileVersion(fileA);
        versionA2.setName("renamed-fileA.jpg");        
        //fileA.addVersion(versionA2);
        
        db.addFileHistory(fileA);
        
        // Create second file
        ChunkEntry chunkB1 = db.createChunk(new byte[] { 3,3,3,3,3,3,3,3,3}, 910, true);
        ChunkEntry chunkB2 = db.createChunk(new byte[] { 4,4,4,4,4,4,4,4,4}, 1112, true);
        
        Content contentB = db.createContent();
        contentB.addChunk(chunkB1);
        contentB.addChunk(chunkB2);   
        contentB.setChecksum(new byte[]{1,1,1,3,3,5,5,5,5});                      
        db.addContent(contentB);

        FileHistory fileB = db.createFileHistory(true);
        
        FileVersion versionB1 = db.createFileVersion(fileB);
        versionB1.setPath("some/other/path");
        versionB1.setName("fileB.jpg");
        versionB1.setContent(contentB);
        //fileB.addVersion(versionB1);
        
        FileVersion versionB2 = db.createFileVersion(fileB);
        versionB2.setName("renamed-fileB.jpg");        
        //fileB.addVersion(versionB2);
        
        db.addFileHistory(fileB);
        
        // Distribute chunks to multichunks
        MultiChunkEntry multiChunkA = db.createMultiChunk();
        multiChunkA.addChunk(chunkA1); // from fileA
        multiChunkA.addChunk(chunkA2); // from fileA
        multiChunkA.addChunk(chunkA3); // from fileA
        multiChunkA.setChecksum(new byte[] {6,6,6,6,6,6,6,6,6});
        db.addMultiChunk(multiChunkA);
        
        MultiChunkEntry multiChunkB = db.createMultiChunk();
        multiChunkB.addChunk(chunkA4); // from fileA
        multiChunkB.addChunk(chunkB1); // from fileB
        multiChunkB.addChunk(chunkB2); // from fileB
        multiChunkB.setChecksum(new byte[] {7,7,7,7,7,7,7,7,7});
        db.addMultiChunk(multiChunkB);

        // Save database
        File dbFile = new File("/tmp/dbfile");
        db.save(dbFile, true, false);        
        
        System.out.println(db);
        return db;
	}
	
	public static Database load1() throws IOException {
		Database db = new Database();
		
		// Load database
        File dbFile = new File("/tmp/dbfile");
		db.load(dbFile, false);

        System.out.println(db);
        
        return db;
	}
	
	public static Database save2(Database db) throws IOException {
		// Create first file
        ChunkEntry chunkC1 = db.createChunk(new byte[] { 99,92,93,4,5,7,8,9,0}, 912,true);
        ChunkEntry chunkC2 = db.createChunk(new byte[] { 99,98,97,6,5,4,3,2,1}, 934, true);
		
        Content contentC = db.createContent();        
        contentC.addChunk(chunkC1);
        contentC.addChunk(chunkC2);
        contentC.setChecksum(new byte[]{95,95,95,4,4,5,5,5,5});              
        db.addContent(contentC);
        
        FileHistory fileC = db.createFileHistory(true);
        
        FileVersion versionC1 = db.createFileVersion(fileC);
        versionC1.setPath("some/path");
        versionC1.setName("fileC.jpg");
        versionC1.setContent(contentC);
                
        db.addFileHistory(fileC);
        
        // Save new stuff
        File dbFile2 = new File("/tmp/dbfile2");
        db.save(dbFile2, false, false);

        System.out.println(db);
        return db;
	}

}
