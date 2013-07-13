package org.syncany.tests.database;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.VectorClock;
import org.syncany.operations.DatabaseVersionUpdateDetector;

public class DatabaseVersionUpdateDetectorTest {

	@Test
	public void testUpdateDetectorScenario1() {	
		System.out.println("SCENARIO 1");
		System.out.println("----------------");
		
		VectorClock v1 = new VectorClock();
		v1.setClock("C", 1);		
		DatabaseVersionHeader d1 = new DatabaseVersionHeader(new Date(1), v1, "C");		
		
		VectorClock v2 = new VectorClock();
		v2.setClock("C", 2);		
		DatabaseVersionHeader d2 = new DatabaseVersionHeader(new Date(2), v2, "C");		
		
		VectorClock v3 = new VectorClock();
		v3.setClock("C", 3);		
		DatabaseVersionHeader d3 = new DatabaseVersionHeader(new Date(3), v3, "C");		
		
		VectorClock v5 = new VectorClock();
		v5.setClock("C", 4);		
		DatabaseVersionHeader d5 = new DatabaseVersionHeader(new Date(4), v5, "C");
		
		VectorClock v7 = new VectorClock();
		v7.setClock("B", 1);		
		v7.setClock("C", 3);		
		DatabaseVersionHeader d7 = new DatabaseVersionHeader(new Date(7), v7, "B");		
		
		VectorClock v8 = new VectorClock();
		v8.setClock("A", 1);		
		v8.setClock("C", 4);		
		DatabaseVersionHeader d8 = new DatabaseVersionHeader(new Date(8), v8, "A");		

		VectorClock v9 = new VectorClock();
		v9.setClock("A", 2);		
		v9.setClock("C", 4);		
		DatabaseVersionHeader d9 = new DatabaseVersionHeader(new Date(9), v9, "A");
		
		VectorClock v10 = new VectorClock();
		v10.setClock("A", 3);		
		v10.setClock("C", 4);		
		DatabaseVersionHeader d10 = new DatabaseVersionHeader(new Date(10), v10, "A");
				
		// Branch (A)
		TreeMap<Long, DatabaseVersionHeader> aDatabaseVersionHeaders = new TreeMap<Long, DatabaseVersionHeader>();
		aDatabaseVersionHeaders.put(d1.getUploadedDate().getTime(), d1);
		aDatabaseVersionHeaders.put(d2.getUploadedDate().getTime(), d2);
		aDatabaseVersionHeaders.put(d3.getUploadedDate().getTime(), d3);
		aDatabaseVersionHeaders.put(d5.getUploadedDate().getTime(), d5);
		aDatabaseVersionHeaders.put(d8.getUploadedDate().getTime(), d8);
		aDatabaseVersionHeaders.put(d9.getUploadedDate().getTime(), d9);
		aDatabaseVersionHeaders.put(d10.getUploadedDate().getTime(), d10);
		
		// Branch (B)
		TreeMap<Long, DatabaseVersionHeader> bDatabaseVersionHeaders = new TreeMap<Long, DatabaseVersionHeader>();
		bDatabaseVersionHeaders.put(d1.getUploadedDate().getTime(), d1);
		bDatabaseVersionHeaders.put(d2.getUploadedDate().getTime(), d2);
		bDatabaseVersionHeaders.put(d3.getUploadedDate().getTime(), d3);
		bDatabaseVersionHeaders.put(d7.getUploadedDate().getTime(), d7);		

		// Branch (C)
		TreeMap<Long, DatabaseVersionHeader> cDatabaseVersionHeaders = new TreeMap<Long, DatabaseVersionHeader>();
		cDatabaseVersionHeaders.put(d1.getUploadedDate().getTime(), d1);
		cDatabaseVersionHeaders.put(d2.getUploadedDate().getTime(), d2);
		cDatabaseVersionHeaders.put(d3.getUploadedDate().getTime(), d3);
		cDatabaseVersionHeaders.put(d5.getUploadedDate().getTime(), d5);		
		
		System.out.println("A:"); printMap(aDatabaseVersionHeaders);
		System.out.println("B:"); printMap(bDatabaseVersionHeaders);
		System.out.println("C:"); printMap(cDatabaseVersionHeaders);
		
		// Compare from B's perspective
		testFromAsPerspective(aDatabaseVersionHeaders, bDatabaseVersionHeaders, cDatabaseVersionHeaders);
		testFromBsPerspective(aDatabaseVersionHeaders, bDatabaseVersionHeaders, cDatabaseVersionHeaders);
		testFromCsPerspective(aDatabaseVersionHeaders, bDatabaseVersionHeaders, cDatabaseVersionHeaders);
	}
	
	@Test
	public void testUpdateDetectorScenario2() {		
		System.out.println("SCENARIO 2");
		System.out.println("----------------");
		
		VectorClock v1 = new VectorClock();
		v1.setClock("C", 1);		
		DatabaseVersionHeader d1 = new DatabaseVersionHeader(new Date(1), v1, "C");		
		
		VectorClock v2 = new VectorClock();
		v2.setClock("C", 2);		
		DatabaseVersionHeader d2 = new DatabaseVersionHeader(new Date(2), v2, "C");		
		
		VectorClock v3 = new VectorClock();
		v3.setClock("C", 3);		
		DatabaseVersionHeader d3 = new DatabaseVersionHeader(new Date(3), v3, "C");		
		
		VectorClock v5 = new VectorClock();
		v5.setClock("C", 4);		
		DatabaseVersionHeader d5 = new DatabaseVersionHeader(new Date(4), v5, "C");
		
		VectorClock v7 = new VectorClock();
		v7.setClock("B", 1);		
		v7.setClock("C", 3);		
		DatabaseVersionHeader d7 = new DatabaseVersionHeader(new Date(7), v7, "B");		
		
		VectorClock v8 = new VectorClock();
		v8.setClock("A", 1);		
		v8.setClock("C", 4);		
		DatabaseVersionHeader d8 = new DatabaseVersionHeader(new Date(8), v8, "A");		

		VectorClock v9 = new VectorClock();
		v9.setClock("A", 2);		
		v9.setClock("C", 4);		
		DatabaseVersionHeader d9 = new DatabaseVersionHeader(new Date(9), v9, "A");
		
		VectorClock v10 = new VectorClock();
		v10.setClock("A", 3);		
		v10.setClock("C", 4);		
		DatabaseVersionHeader d10 = new DatabaseVersionHeader(new Date(10), v10, "A");

		VectorClock v11 = new VectorClock();
		v11.setClock("A", 4);		
		v11.setClock("C", 4);		
		DatabaseVersionHeader d11 = new DatabaseVersionHeader(new Date(11), v11, "A");

		VectorClock v12 = new VectorClock();
		v12.setClock("A", 5);		
		v12.setClock("C", 4);		
		DatabaseVersionHeader d12 = new DatabaseVersionHeader(new Date(12), v12, "A");
		
		VectorClock v13 = new VectorClock();
		v13.setClock("A", 3);		
		v13.setClock("C", 5);		
		DatabaseVersionHeader d13 = new DatabaseVersionHeader(new Date(13), v13, "C");
		
		VectorClock v14 = new VectorClock();
		v14.setClock("A", 3);		
		v14.setClock("C", 6);		
		DatabaseVersionHeader d14 = new DatabaseVersionHeader(new Date(14), v14, "C");
		
		VectorClock v15 = new VectorClock();
		v15.setClock("A", 3);		
		v15.setClock("C", 7);		
		DatabaseVersionHeader d15 = new DatabaseVersionHeader(new Date(15), v15, "C");		

		VectorClock v16 = new VectorClock();
		v16.setClock("A", 3);
		v16.setClock("B", 2);
		v16.setClock("C", 4);		
		DatabaseVersionHeader d16 = new DatabaseVersionHeader(new Date(16), v16, "B");	
		
		VectorClock v17 = new VectorClock();
		v17.setClock("A", 3);
		v17.setClock("B", 3);
		v17.setClock("C", 4);		
		DatabaseVersionHeader d17 = new DatabaseVersionHeader(new Date(17), v17, "B");			
		
		VectorClock v18 = new VectorClock();
		v18.setClock("A", 3);
		v18.setClock("B", 4);
		v18.setClock("C", 4);		
		DatabaseVersionHeader d18 = new DatabaseVersionHeader(new Date(18), v18, "B");			
		
		VectorClock v19 = new VectorClock();
		v19.setClock("A", 6);
		v19.setClock("C", 4);		
		DatabaseVersionHeader d19 = new DatabaseVersionHeader(new Date(19), v19, "A");	
		
		VectorClock v20 = new VectorClock();
		v20.setClock("A", 3);
		v20.setClock("B", 5);
		v20.setClock("C", 4);		
		DatabaseVersionHeader d20 = new DatabaseVersionHeader(new Date(20), v20, "B");
		
		VectorClock v21 = new VectorClock();
		v21.setClock("A", 3);
		v21.setClock("C", 8);		
		DatabaseVersionHeader d21 = new DatabaseVersionHeader(new Date(21), v21, "C");	
		
		// Branch (A)
		TreeMap<Long, DatabaseVersionHeader> aDatabaseVersionHeaders = new TreeMap<Long, DatabaseVersionHeader>();
		aDatabaseVersionHeaders.put(d11.getUploadedDate().getTime(), d11);
		aDatabaseVersionHeaders.put(d12.getUploadedDate().getTime(), d12);
		aDatabaseVersionHeaders.put(d19.getUploadedDate().getTime(), d19);
		
		// Branch (B)
		TreeMap<Long, DatabaseVersionHeader> bDatabaseVersionHeaders = new TreeMap<Long, DatabaseVersionHeader>();
		bDatabaseVersionHeaders.put(d1.getUploadedDate().getTime(), d1);
		bDatabaseVersionHeaders.put(d2.getUploadedDate().getTime(), d2);
		bDatabaseVersionHeaders.put(d3.getUploadedDate().getTime(), d3);
		bDatabaseVersionHeaders.put(d7.getUploadedDate().getTime(), d7);		
		bDatabaseVersionHeaders.put(d16.getUploadedDate().getTime(), d16);		
		bDatabaseVersionHeaders.put(d17.getUploadedDate().getTime(), d17);		
		bDatabaseVersionHeaders.put(d18.getUploadedDate().getTime(), d18);		
		bDatabaseVersionHeaders.put(d20.getUploadedDate().getTime(), d20);		

		// Branch (C)
		TreeMap<Long, DatabaseVersionHeader> cDatabaseVersionHeaders = new TreeMap<Long, DatabaseVersionHeader>();
		cDatabaseVersionHeaders.put(d13.getUploadedDate().getTime(), d13);
		cDatabaseVersionHeaders.put(d14.getUploadedDate().getTime(), d14);
		cDatabaseVersionHeaders.put(d15.getUploadedDate().getTime(), d15);
		cDatabaseVersionHeaders.put(d21.getUploadedDate().getTime(), d21);		
		
		System.out.println("A:"); printMap(aDatabaseVersionHeaders);
		System.out.println("B:"); printMap(bDatabaseVersionHeaders);
		System.out.println("C:"); printMap(cDatabaseVersionHeaders);
		
		// Compare from B's perspective
		testFromBsPerspective(aDatabaseVersionHeaders, bDatabaseVersionHeaders, cDatabaseVersionHeaders);
	}	
	
	@Test
	public void testUpdateDetectorScenario3() {	
		System.out.println("SCENARIO 3");
		System.out.println("----------------");
		
		VectorClock v1 = new VectorClock();
		v1.setClock("C", 1);		
		DatabaseVersionHeader d1 = new DatabaseVersionHeader(new Date(1), v1, "C");		
		
		VectorClock v2 = new VectorClock();
		v2.setClock("C", 2);		
		DatabaseVersionHeader d2 = new DatabaseVersionHeader(new Date(2), v2, "C");		
		
		VectorClock v3 = new VectorClock();
		v3.setClock("C", 3);		
		DatabaseVersionHeader d3 = new DatabaseVersionHeader(new Date(3), v3, "C");		
		
		VectorClock v5 = new VectorClock();
		v5.setClock("C", 4);		
		DatabaseVersionHeader d5 = new DatabaseVersionHeader(new Date(4), v5, "C");
		
		VectorClock v7 = new VectorClock();
		v7.setClock("B", 1);		
		v7.setClock("C", 3);		
		DatabaseVersionHeader d7 = new DatabaseVersionHeader(new Date(7), v7, "B");		
		
		VectorClock v8 = new VectorClock();
		v8.setClock("A", 1);		
		v8.setClock("C", 4);		
		DatabaseVersionHeader d8 = new DatabaseVersionHeader(new Date(8), v8, "A");		

		VectorClock v9 = new VectorClock();
		v9.setClock("A", 2);		
		v9.setClock("C", 4);		
		DatabaseVersionHeader d9 = new DatabaseVersionHeader(new Date(9), v9, "A");
		
		VectorClock v10 = new VectorClock();
		v10.setClock("A", 3);		
		v10.setClock("C", 4);		
		DatabaseVersionHeader d10 = new DatabaseVersionHeader(new Date(10), v10, "A");
				
		// Branch (A)
		TreeMap<Long, DatabaseVersionHeader> aDatabaseVersionHeaders = new TreeMap<Long, DatabaseVersionHeader>();
		aDatabaseVersionHeaders.put(d1.getUploadedDate().getTime(), d1);
		aDatabaseVersionHeaders.put(d2.getUploadedDate().getTime(), d2);
		aDatabaseVersionHeaders.put(d3.getUploadedDate().getTime(), d3);
		aDatabaseVersionHeaders.put(d5.getUploadedDate().getTime(), d5);
		aDatabaseVersionHeaders.put(d8.getUploadedDate().getTime(), d8);
		aDatabaseVersionHeaders.put(d9.getUploadedDate().getTime(), d9);
		aDatabaseVersionHeaders.put(d10.getUploadedDate().getTime(), d10);
		
		// Branch (B)
		TreeMap<Long, DatabaseVersionHeader> bDatabaseVersionHeaders = new TreeMap<Long, DatabaseVersionHeader>();
		//bDatabaseVersionHeaders.put(d1.getUploadedDate().getTime(), d1);
		//bDatabaseVersionHeaders.put(d2.getUploadedDate().getTime(), d2);
		//bDatabaseVersionHeaders.put(d3.getUploadedDate().getTime(), d3);
		bDatabaseVersionHeaders.put(d7.getUploadedDate().getTime(), d7);		

		// Branch (C)
		TreeMap<Long, DatabaseVersionHeader> cDatabaseVersionHeaders = new TreeMap<Long, DatabaseVersionHeader>();
		/*cDatabaseVersionHeaders.put(d1.getUploadedDate().getTime(), d1);
		cDatabaseVersionHeaders.put(d2.getUploadedDate().getTime(), d2);
		cDatabaseVersionHeaders.put(d3.getUploadedDate().getTime(), d3);
		cDatabaseVersionHeaders.put(d5.getUploadedDate().getTime(), d5);	*/	
		
		System.out.println("A:"); printMap(aDatabaseVersionHeaders);
		System.out.println("B:"); printMap(bDatabaseVersionHeaders);
		System.out.println("C:"); printMap(cDatabaseVersionHeaders);
		
		// Compare from A's perspective
		testFromAsPerspective(aDatabaseVersionHeaders, bDatabaseVersionHeaders, cDatabaseVersionHeaders);
	}	
	
	private void printMap(Map<?, ?> someMap) {
		for (Map.Entry<?, ?> entry : someMap.entrySet()) {
			System.out.println("- "+entry.getKey()+": "+entry.getValue());
		}
	}
	
	private void testFromAsPerspective(TreeMap<Long, DatabaseVersionHeader> aDatabaseVersionHeaders,
			TreeMap<Long, DatabaseVersionHeader> bDatabaseVersionHeaders, TreeMap<Long, DatabaseVersionHeader> cDatabaseVersionHeaders) {

		String localMachineName = "A";
		
		TreeMap<Long, DatabaseVersionHeader> localDatabaseVersionHeaders = new TreeMap<Long, DatabaseVersionHeader>();
		TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> remoteDatabaseVersionHeaders = new TreeMap<String, TreeMap<Long, DatabaseVersionHeader>>();
		
		localDatabaseVersionHeaders = aDatabaseVersionHeaders;
		remoteDatabaseVersionHeaders.put("B", bDatabaseVersionHeaders);
		//remoteDatabaseVersionHeaders.put("C", cDatabaseVersionHeaders);
		
		DatabaseVersionUpdateDetector comp = new DatabaseVersionUpdateDetector();
		DatabaseVersionHeader lastCommonHeader = comp.findLastCommonDatabaseVersionHeader(localDatabaseVersionHeaders, remoteDatabaseVersionHeaders);		
		Map<String, DatabaseVersionHeader> firstConflictingDatabaseVersionHeaders = comp.findFirstConflictingDatabaseVersionHeader(lastCommonHeader, localMachineName, localDatabaseVersionHeaders, remoteDatabaseVersionHeaders);
		Map<String, DatabaseVersionHeader> winningFirstConflictingDatabaseVersionHeaders = comp.findWinningFirstConflictingDatabaseVersionHeaders(firstConflictingDatabaseVersionHeaders);
		System.out.println("lastCommonHeader = " +lastCommonHeader);		
		System.out.println("firstConflictingVersion = "); printMap(firstConflictingDatabaseVersionHeaders);
		System.out.println("winningFirstConflictingVersion = "); printMap(winningFirstConflictingDatabaseVersionHeaders);
	}
	
	private void testFromBsPerspective(TreeMap<Long, DatabaseVersionHeader> aDatabaseVersionHeaders,
			TreeMap<Long, DatabaseVersionHeader> bDatabaseVersionHeaders, TreeMap<Long, DatabaseVersionHeader> cDatabaseVersionHeaders) {

		String localMachineName = "B";
		
		TreeMap<Long, DatabaseVersionHeader> localDatabaseVersionHeaders = new TreeMap<Long, DatabaseVersionHeader>();
		TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> remoteDatabaseVersionHeaders = new TreeMap<String, TreeMap<Long, DatabaseVersionHeader>>();
		
		localDatabaseVersionHeaders = bDatabaseVersionHeaders;
		remoteDatabaseVersionHeaders.put("A", aDatabaseVersionHeaders);
		remoteDatabaseVersionHeaders.put("C", cDatabaseVersionHeaders);
		
		DatabaseVersionUpdateDetector comp = new DatabaseVersionUpdateDetector();
		DatabaseVersionHeader lastCommonHeader = comp.findLastCommonDatabaseVersionHeader(localDatabaseVersionHeaders, remoteDatabaseVersionHeaders);		
		Map<String, DatabaseVersionHeader> firstConflictingDatabaseVersionHeaders = comp.findFirstConflictingDatabaseVersionHeader(lastCommonHeader, localMachineName, localDatabaseVersionHeaders, remoteDatabaseVersionHeaders);
		Map<String, DatabaseVersionHeader> winningFirstConflictingDatabaseVersionHeaders = comp.findWinningFirstConflictingDatabaseVersionHeaders(firstConflictingDatabaseVersionHeaders);
				
		System.out.println("lastCommonHeader = " +lastCommonHeader);		
		System.out.println("firstConflictingVersion = "); printMap(firstConflictingDatabaseVersionHeaders);
		System.out.println("winningFirstConflictingVersion = "); printMap(winningFirstConflictingDatabaseVersionHeaders);
	}
	
	private void testFromCsPerspective(TreeMap<Long, DatabaseVersionHeader> aDatabaseVersionHeaders,
			TreeMap<Long, DatabaseVersionHeader> bDatabaseVersionHeaders, TreeMap<Long, DatabaseVersionHeader> cDatabaseVersionHeaders) {

		String localMachineName = "C";
		
		TreeMap<Long, DatabaseVersionHeader> localDatabaseVersionHeaders = new TreeMap<Long, DatabaseVersionHeader>();
		TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> remoteDatabaseVersionHeaders = new TreeMap<String, TreeMap<Long, DatabaseVersionHeader>>();
		
		localDatabaseVersionHeaders = cDatabaseVersionHeaders;
		remoteDatabaseVersionHeaders.put("A", aDatabaseVersionHeaders);
		remoteDatabaseVersionHeaders.put("B", bDatabaseVersionHeaders);
		
		DatabaseVersionUpdateDetector comp = new DatabaseVersionUpdateDetector();
		DatabaseVersionHeader lastCommonHeader = comp.findLastCommonDatabaseVersionHeader(localDatabaseVersionHeaders, remoteDatabaseVersionHeaders);		
		Map<String, DatabaseVersionHeader> firstConflictingDatabaseVersionHeaders = comp.findFirstConflictingDatabaseVersionHeader(lastCommonHeader, localMachineName, localDatabaseVersionHeaders, remoteDatabaseVersionHeaders);
		Map<String, DatabaseVersionHeader> winningFirstConflictingDatabaseVersionHeaders = comp.findWinningFirstConflictingDatabaseVersionHeaders(firstConflictingDatabaseVersionHeaders);
				
		System.out.println("lastCommonHeader = " +lastCommonHeader);		
		System.out.println("firstConflictingVersion = "); printMap(firstConflictingDatabaseVersionHeaders);
		System.out.println("winningFirstConflictingVersion = "); printMap(winningFirstConflictingDatabaseVersionHeaders);
	}
}
