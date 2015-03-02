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
package org.syncany.tests.unit.database;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.syncany.database.VectorClock;
import org.syncany.database.VectorClock.VectorClockComparison;

public class VectorClockTest {
	@Test
	public void testNormalVectorClockUsage() {
		VectorClock vc = new  VectorClock();
		
		vc.setClock("UnitA", 1);
		vc.setClock("UnitB", 2);
		vc.setClock("UnitC", 3);
		
		assertEquals("Expected clock value to be different.", 1L, (long) vc.getClock("UnitA"));
		assertEquals("Expected clock value to be different.", 2L, (long) vc.getClock("UnitB"));
		assertEquals("Expected clock value to be different.", 3L, (long) vc.getClock("UnitC"));
	}
		
	@Test
	public void testIncrementUnit() {
		VectorClock vc = new  VectorClock();
		
		vc.setClock("UnitA", 1);		
		vc.incrementClock("UnitA"); // 2
		vc.incrementClock("UnitA"); // 3
		
		vc.setClock("UnitB", 2);
		vc.incrementClock("UnitB"); // 3
		
		assertEquals("Expected clock value to be different.", 3L, (long) vc.getClock("UnitA"));
		assertEquals("Expected clock value to be different.", 3L, (long) vc.getClock("UnitB"));
	}	
	
	@Test
	public void testCompareEqualClocks() {
		VectorClock vc1 = new  VectorClock();
		vc1.setClock("UnitA", 4L);
		vc1.setClock("UnitB", 5L);
		
		VectorClock vc2 = new  VectorClock();
		vc2.setClock("UnitA", 4L); // same
		vc2.setClock("UnitB", 5L); // same
		
		assertEquals("Expected clock 1 and 2 to be equal.", VectorClockComparison.EQUAL, VectorClock.compare(vc1, vc2));
		assertEquals("Expected clock 2 and 1 to be equal.", VectorClockComparison.EQUAL, VectorClock.compare(vc2, vc1));
	}		
	
	@Test
	public void testCompareSmallerClocksWithSameUnitCount() {
		VectorClock vc1 = new  VectorClock();
		vc1.setClock("UnitA", 4L);
		vc1.setClock("UnitB", 5L);
		
		VectorClock vc2 = new  VectorClock();
		vc2.setClock("UnitA", 4L);
		vc2.setClock("UnitB", 100000L); // greater!
		
		assertEquals("Expected clock 1 to be smaller than clock 2.", VectorClockComparison.SMALLER, VectorClock.compare(vc1, vc2));
		assertEquals("Expected clock 2 to be greater than clock 1.", VectorClockComparison.GREATER, VectorClock.compare(vc2, vc1));
	}	
	
	@Test
	public void testCompareSmallerClocksWithDifferentUnitCount() {
		VectorClock vc1 = new  VectorClock();
		vc1.setClock("UnitA", 4L);
		vc1.setClock("UnitB", 5L);
		
		VectorClock vc2 = new  VectorClock();
		vc2.setClock("UnitA", 4L); // same
		vc2.setClock("UnitB", 5L); // same
		vc2.setClock("UnitC", 100000L); // not in vc1
		
		assertEquals("Expected clock 1 to be smaller than clock 2.", VectorClockComparison.SMALLER, VectorClock.compare(vc1, vc2));
		assertEquals("Expected clock 2 to be greater than clock 1.", VectorClockComparison.GREATER, VectorClock.compare(vc2, vc1));
	}			
	
	@Test
	public void testCompareGreaterClocksWithSameUnitCount() {
		VectorClock vc1 = new  VectorClock();
		vc1.setClock("UnitA", 4L);
		vc1.setClock("UnitB", 5L);
		
		VectorClock vc2 = new  VectorClock();
		vc2.setClock("UnitA", 4L);
		vc2.setClock("UnitB", 100000L); // greater!
		
		assertEquals("Expected clock 2 to be greater than clock 1.", VectorClockComparison.GREATER, VectorClock.compare(vc2, vc1));
		assertEquals("Expected clock 1 to be smaller than clock 2.", VectorClockComparison.SMALLER, VectorClock.compare(vc1, vc2));
	}	
	
	@Test
	public void testCompareGreaterClocksWithDifferentUnitCount() {
		VectorClock vc1 = new  VectorClock();
		vc1.setClock("UnitA", 4L);
		vc1.setClock("UnitB", 5L);
		
		VectorClock vc2 = new  VectorClock();
		vc2.setClock("UnitA", 4L); // same
		vc2.setClock("UnitB", 5L); // same
		vc2.setClock("UnitC", 1L); // not in vc1
		
		assertEquals("Expected clock 2 to be greater than clock 1.", VectorClockComparison.GREATER, VectorClock.compare(vc2, vc1));
		assertEquals("Expected clock 1 to be smaller than clock 2.", VectorClockComparison.SMALLER, VectorClock.compare(vc1, vc2));
	}	
	
	@Test
	public void testSimultaneousClocks() {
		VectorClock vc1 = new  VectorClock();
		vc1.setClock("UnitA", 4L);
		vc1.setClock("UnitB", 5L);
		
		VectorClock vc2 = new  VectorClock();
		vc2.setClock("UnitC", 1L); 
		vc2.setClock("UnitD", 2L); 
		
		assertEquals("Expected clock 1 and 2 to be simulataneous.", VectorClockComparison.SIMULTANEOUS, VectorClock.compare(vc1, vc2));
		assertEquals("Expected clock 2 and 1 to be simulataneous.", VectorClockComparison.SIMULTANEOUS, VectorClock.compare(vc2, vc1));
	}		
	
	@Test
	public void testIncrementNonExistingUnit() {
		VectorClock vc = new  VectorClock();
		
		vc.incrementClock("NonExistingUnit");
		
		assertEquals("Expected clock value to be different.", 1L, (long) vc.getClock("NonExistingUnit"));
	}	
	
	@Test
	public void testNonExistingVectorClockUsage() {
		VectorClock vc = new  VectorClock();
		
		assertEquals("Expected clock value to be different.", 0L, (long) vc.getClock("NonExistingUnit"));
	}	
		
	@Test
	public void testToString() {
		VectorClock vc1 = new  VectorClock();
		vc1.setClock("UnitBBB", 5L);
		vc1.setClock("UnitAAA", 4L);
		
		assertEquals("Expected different serialization", "(UnitAAA4,UnitBBB5)", vc1.toString());
	}	
	
	@Test
	public void testParseClock() {
		VectorClock vc1 = VectorClock.parseVectorClock("(UnitBBB5,UnitAAA4)");
		
		assertEquals(4L, (long) vc1.get("UnitAAA"));
		assertEquals(5L, (long) vc1.get("UnitBBB"));
	}	
	
	@Test
	public void testClone() {
		VectorClock vc1 = new  VectorClock();
		vc1.setClock("UnitA", 4L);
		vc1.setClock("UnitB", 5L);

		VectorClock vc2 = vc1.clone();
				
		assertEquals("Expected clock value of cloned clock to be different.", 4L, (long) vc2.getClock("UnitA"));
		assertEquals("Expected clock value of cloned clock to be different.", 5L, (long) vc2.getClock("UnitB"));
	}	
}
