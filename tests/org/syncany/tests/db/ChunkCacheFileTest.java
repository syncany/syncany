package org.syncany.tests.db;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.syncany.db.ChunkCache;
import org.syncany.db.CloneChunk;
import org.syncany.tests.TestSettings;


/** 
 * TODO: logical success/failure of this test are missing
 * remove/commenct Sysos and build in asserts to use JUnit notation
 *  
 */
public class ChunkCacheFileTest {

	@BeforeClass
	public static void init(){
		TestSettings.getInstance().createSettingsInstance();
	}
	
	@Test
	public void simpleReadTest() throws IOException {
		ChunkCache.initFile();
		ChunkCache cache1 = generateCache(5);
		System.out.println("Cache1: " + cache1);
		System.out.println("Initializing ChunkCache");
		ChunkCache cache2 = new ChunkCache();
		System.out.println("Cache2: " + cache2);
	}
	
	@Test
	public void extremeReadTest() throws IOException {
		ChunkCache.initFile();
		ChunkCache cache1 = generateCache(10000);
		System.out.println("Initializing ChunkCache");
		ChunkCache cache2 = new ChunkCache();
		System.out.println("Cache1: " + cache1.getSize());
		System.out.println("Cache2: " + cache2.getSize());
	}
	
	private byte[] randomizeChecksum(){
		byte[] bytes = new byte[32];
		for(int i = 0; i < 32; i++){
			bytes[i] = (byte)((int)(Math.random()*255) & 0xff);
		}
		
		return bytes;
	}
	
	private ChunkCache generateCache(int maxChunks){
		ChunkCache cache = new ChunkCache();
		
		System.out.println("Generating ChunkCache with " + maxChunks + " Chunks");
		for(int i = 0; i < maxChunks; i++){
			CloneChunk c = new CloneChunk(randomizeChecksum());
			c.setMetaId(randomizeChecksum());
			cache.add(c);
		}
		
		return cache;
	}

}
