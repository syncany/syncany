package org.syncany.tests.database;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;
import org.syncany.database.VectorClock;
import org.syncany.operations.DatabaseVersionUpdateDetector;

public class UpdateDetectorTest {
	@Test
	@Ignore
	public void testReconcileThreeClientsNoConflict() {
		VectorClock vcA = new VectorClock();		
		vcA.setClock("A", 1L);
		vcA.setClock("B", 1L);
		vcA.setClock("C", 1L);
		
		VectorClock vcB = new VectorClock();		
		vcB.setClock("A", 1L);
		vcB.setClock("B", 2L);
		vcB.setClock("C", 1L);
		
		VectorClock vcC = new VectorClock();		
		vcC.setClock("A", 1L);
		vcC.setClock("B", 2L);
		vcC.setClock("C", 2L);
		
		// A < B < C
		DatabaseVersionUpdateDetector reconciliator = new DatabaseVersionUpdateDetector(); // TODO BROKEN!
		//VectorClock newestClock = reconciliator.detectUpdates(vcA, vcB, vcC);
		
		//assertEquals("Expected newest vector clock does not match.", vcC, newestClock);
		//assertEquals("No conflicts expected.", 0, reconciliator.getPossibleConflicts().size());		
	}
	
	@Test
	@Ignore
	public void testReconcileThreeClientsOneConflict() {
		VectorClock vcA = new VectorClock();		
		vcA.setClock("A", 1L);
		vcA.setClock("B", 1L);
		vcA.setClock("C", 1L);
		
		VectorClock vcB = new VectorClock();		
		vcB.setClock("A", 1L);
		vcB.setClock("B", 2L);
		vcB.setClock("C", 1L);
		
		VectorClock vcC = new VectorClock();		
		vcC.setClock("A", 1L);
		vcC.setClock("B", 1L);
		vcC.setClock("C", 2L);
		
		// A < B, A < C, B || C
		DatabaseVersionUpdateDetector reconciliator = new DatabaseVersionUpdateDetector(); // TODO BROKEN
		//VectorClock newestClock = reconciliator.detectUpdates(vcA, vcB, vcC); // TODO BROKEN
		
		//assertEquals("Expected newest vector clock does not match.", vcA, newestClock);
		//assertEquals("One conflict expected.", 1, reconciliator.getPossibleConflicts().size());		

	}
	
	@Test
	@Ignore
	public void testReconcileFourClientsOneConflict() {
		VectorClock vcA = new VectorClock();		
		vcA.setClock("A", 3L);
		vcA.setClock("B", 2L);
		vcA.setClock("C", 2L);
		vcA.setClock("D", 3L);
		
		VectorClock vcB = new VectorClock();		
		vcB.setClock("A", 3L);
		vcB.setClock("B", 3L);
		vcB.setClock("C", 2L);
		vcB.setClock("D", 3L);
		
		VectorClock vcC = new VectorClock();		
		vcC.setClock("A", 2L);
		vcC.setClock("B", 2L);
		vcC.setClock("C", 5L);
		vcC.setClock("D", 3L);
		
		VectorClock vcD = new VectorClock();		
		vcD.setClock("A", 2L);
		vcD.setClock("B", 2L);
		vcD.setClock("C", 2L);		
		vcD.setClock("D", 3L);
		
		new DatabaseVersionUpdateDetector().detectUpdates(vcA, vcB, vcC, vcD); // TODO BROKEN
		// D < A < B, D < C, C || B, C || A
	}	
	
	@Test
	@Ignore
	public void testDatabaseVersionsReconciliation() {
		
		
	}
}
