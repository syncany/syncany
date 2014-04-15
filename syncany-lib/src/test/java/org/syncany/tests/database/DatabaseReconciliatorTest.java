/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.database;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.operations.down.DatabaseBranch;
import org.syncany.operations.down.DatabaseBranches;
import org.syncany.operations.down.DatabaseReconciliator;
import org.syncany.tests.util.TestDatabaseUtil;

public class DatabaseReconciliatorTest {	
	@Test
	public void testUpdateDetectorConflict1AtMachineB() throws Exception {	
		System.out.println("CONFLICT 1");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "B";
		DatabaseVersionHeader currentLocalVersion = null;
		DatabaseBranches allBranches = new DatabaseBranches();
		 
		// A
		allBranches.put("A", TestDatabaseUtil.createBranch(new String[] {
			"A/(A1,C4)/T=8",
			"A/(A2,C4)/T=9",
			"A/(A3,C4)/T=10"								
		}));
		
		// B
		allBranches.put("B", TestDatabaseUtil.createBranch(new String[] {
			"C/(C1)/T=1",
			"C/(C2)/T=2",
			"C/(C3)/T=3",				
			"B/(B1,C3)/T=7"								
		}));
		
		// C
		allBranches.put("C", TestDatabaseUtil.createBranch(new String[] {
			"C/(C4)/T=5"
		}));		
				
		/// Expected results ///
		TestResult expectedTestResult = new TestResult();
		
		expectedTestResult.lastCommonHeader = TestDatabaseUtil.createFromString("C/(C3)/T=3");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "C/(C4)/T=5",
			"B", "B/(B1,C3)/T=7",
			"C", "C/(C4)/T=5"			
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "C/(C4)/T=5",
			"C", "C/(C4)/T=5"		
		});
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A3,C4)/T=10"
		}).firstEntry();
				
		/// Perform test ///
		testFromMachinePerspective(localMachineName, currentLocalVersion, allBranches, expectedTestResult);
	}			
	
	@Test
	public void testUpdateDetectorConflict2AtMachineA() throws Exception {	
		System.out.println("CONFLICT 2");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "A";

		DatabaseVersionHeader currentLocalVersion = null;
		DatabaseBranches allBranches = new DatabaseBranches();
		
		// A
		allBranches.put("A", TestDatabaseUtil.createBranch(new String[] {
			"C/(C1)/T=1",
			"C/(C2)/T=2",
			"C/(C3)/T=3",
			"C/(C4)/T=5",
			"A/(A1,C4)/T=8",
			"A/(A2,C4)/T=9",
			"A/(A3,C4)/T=10"								
		}));
		
		// B
		allBranches.put("B", TestDatabaseUtil.createBranch(new String[] {
			"B/(B1,C3)/T=7"								
		}));
				
		/// Expected results ///
		TestResult expectedTestResult = new TestResult();
		
		expectedTestResult.lastCommonHeader = TestDatabaseUtil.createFromString("C/(C3)/T=3");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "C/(C4)/T=5",
			"B", "B/(B1,C3)/T=7"
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "C/(C4)/T=5"
		});
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A3,C4)/T=10"
		}).firstEntry();
				
		/// Perform test ///
		testFromMachinePerspective(localMachineName, currentLocalVersion, allBranches, expectedTestResult);
	}	
	
	@Test
	public void testUpdateDetectorConflict3AtMachineC() throws Exception {	
		System.out.println("CONFLICT 3");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "C";

		DatabaseVersionHeader currentLocalVersion = null;
		DatabaseBranches allBranches = new DatabaseBranches();
		
		// A
		allBranches.put("A", TestDatabaseUtil.createBranch(new String[] {
			"A/(A1,C4)/T=8",
			"A/(A2,C4)/T=9",
			"A/(A3,C4)/T=10"								
		}));
		
		// B
		allBranches.put("B", TestDatabaseUtil.createBranch(new String[] {			
			"B/(B1,C3)/T=7"								
		}));
		
		// C
		allBranches.put("C", TestDatabaseUtil.createBranch(new String[] {
			"C/(C1)/T=1",
			"C/(C2)/T=2",
			"C/(C3)/T=3",				
			"C/(C4)/T=5"
		}));		

		/// Expected results ///x
		TestResult expectedTestResult = new TestResult();
		
		expectedTestResult.lastCommonHeader = TestDatabaseUtil.createFromString("C/(C3)/T=3");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "C/(C4)/T=5",
			"B", "B/(B1,C3)/T=7",
			"C", "C/(C4)/T=5"
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "C/(C4)/T=5",
			"C", "C/(C4)/T=5"
		});
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A3,C4)/T=10"
		}).firstEntry();
				
		/// Perform test ///
		testFromMachinePerspective(localMachineName, currentLocalVersion, allBranches, expectedTestResult);
	}			
	
	@Test
	public void testUpdateDetectorConflict4AtMachineB() throws Exception {	
		System.out.println("CONFLICT 4");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "B";

		DatabaseVersionHeader currentLocalVersion = null;
		DatabaseBranches allBranches = new DatabaseBranches();
		
		// A
		allBranches.put("A", TestDatabaseUtil.createBranch(new String[] {
			"A/(A4,C4)/T=11",
			"A/(A5,C4)/T=12", // db-a-5
			"A/(A6,C4)/T=19", // db-a-6
		}));
		
		// B
		allBranches.put("B", TestDatabaseUtil.createBranch(new String[] {			
			"C/(C1)/T=1",
			"C/(C2)/T=2",
			"C/(C3)/T=3",				
			"C/(C4)/T=5",
			"A/(A1,C4)/T=8",
			"A/(A2,C4)/T=9",
			"A/(A3,C4)/T=10",
			"B/(A3,B2,C4)/T=16",
			"B/(A3,B3,C4)/T=17",
			"B/(A3,B4,C4)/T=18",
			"B/(A3,B5,C4)/T=20",
		}));
		
		// C
		allBranches.put("C", TestDatabaseUtil.createBranch(new String[] {
			"C/(A3,C5)/T=13", // db-c-5
			"C/(A3,C6)/T=14",
			"C/(A3,C7)/T=15", // db-c-7
			"C/(A3,C8)/T=21", // db-c-8
		}));		

		/// Expected results ///
		TestResult expectedTestResult = new TestResult();
		
		expectedTestResult.lastCommonHeader = TestDatabaseUtil.createFromString("A/(A3,C4)/T=10");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A4,C4)/T=11",
			"B", "B/(A3,B2,C4)/T=16",
			"C", "C/(A3,C5)/T=13"
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A4,C4)/T=11"
		});
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A6,C4)/T=19"
		}).firstEntry();
				
		/// Perform test ///
		testFromMachinePerspective(localMachineName, currentLocalVersion, allBranches, expectedTestResult);
	}			
	
	@Test
	public void testUpdateDetectorConflict5AtMachineA() throws Exception {	
		System.out.println("CONFLICT 5");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "A";

		DatabaseVersionHeader currentLocalVersion = null;
		DatabaseBranches allBranches = new DatabaseBranches();
		
		// A
		allBranches.put("A", TestDatabaseUtil.createBranch(new String[] {
			"C/(C1)/T=1",
			"C/(C2)/T=2",
			"C/(C3)/T=3",				
			"C/(C4)/T=5",
			"A/(A1,C4)/T=8",
			"A/(A2,C4)/T=9",
			"A/(A3,C4)/T=10",
			"A/(A4,C4)/T=11",
			"A/(A5,C4)/T=12", 
			"A/(A6,C4)/T=19", 
		}));
		
		// B
		allBranches.put("B", TestDatabaseUtil.createBranch(new String[] {			
			"B/(A3,B2,C4)/T=16",
			"B/(A3,B3,C4)/T=17",
			"B/(A3,B4,C4)/T=18", // db-b-4
			"B/(A3,B5,C4)/T=20", // db-b-5
		}));
		
		// C
		allBranches.put("C", TestDatabaseUtil.createBranch(new String[] {
			"C/(A3,C5)/T=13", // db-c-5
			"C/(A3,C6)/T=14",
			"C/(A3,C7)/T=15", // db-c-7
			"C/(A3,C8)/T=21", // db-c-8
		}));		

		/// Expected results ///
		TestResult expectedTestResult = new TestResult();
		
		expectedTestResult.lastCommonHeader = TestDatabaseUtil.createFromString("A/(A3,C4)/T=10");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A4,C4)/T=11",
			"B", "B/(A3,B2,C4)/T=16",
			"C", "C/(A3,C5)/T=13"
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A4,C4)/T=11"
		});
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A6,C4)/T=19"
		}).firstEntry();		
				
		/// Perform test ///
		testFromMachinePerspective(localMachineName, currentLocalVersion, allBranches, expectedTestResult);
	}				
	
	@Test
	public void testUpdateDetectorConflict6AtMachineC() throws Exception {	
		System.out.println("CONFLICT 6");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "C";

		DatabaseVersionHeader currentLocalVersion = null;
		DatabaseBranches allBranches = new DatabaseBranches();
		
		// A
		allBranches.put("A", TestDatabaseUtil.createBranch(new String[] {
			"A/(A4,C4)/T=11",
			"A/(A5,C4)/T=12", // db-a-5
			"A/(A6,C4)/T=19", // db-a-6
		}));
		
		// B
		allBranches.put("B", TestDatabaseUtil.createBranch(new String[] {			
			"B/(A3,B2,C4)/T=16",
			"B/(A3,B3,C4)/T=17",
			"B/(A3,B4,C4)/T=18", // db-b-4
			"B/(A3,B5,C4)/T=20", // db-b-5
		}));
		
		// C
		allBranches.put("C", TestDatabaseUtil.createBranch(new String[] {
			"C/(C1)/T=1",
			"C/(C2)/T=2",
			"C/(C3)/T=3",				
			"C/(C4)/T=5",
			"A/(A1,C4)/T=8",
			"A/(A2,C4)/T=9",
			"A/(A3,C4)/T=10",				
			"C/(A3,C5)/T=13", 
			"C/(A3,C6)/T=14",
			"C/(A3,C7)/T=15", 
			"C/(A3,C8)/T=21", 
		}));		

		/// Expected results ///
		TestResult expectedTestResult = new TestResult();
		
		expectedTestResult.lastCommonHeader = TestDatabaseUtil.createFromString("A/(A3,C4)/T=10");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A4,C4)/T=11",
			"B", "B/(A3,B2,C4)/T=16",
			"C", "C/(A3,C5)/T=13"
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A4,C4)/T=11"
		});
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A6,C4)/T=19"
		}).firstEntry();		
				
		/// Perform test ///
		testFromMachinePerspective(localMachineName, currentLocalVersion, allBranches, expectedTestResult);
	}				
	
	@Test
	public void testUpdateDetectorConflict7AtMachineC() throws Exception {	
		System.out.println("CONFLICT 7");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "C";
		
		DatabaseVersionHeader currentLocalVersion = null;

		DatabaseBranches allBranches = new DatabaseBranches();
		
		// B
		allBranches.put("B", TestDatabaseUtil.createBranch(new String[] {
			// TODO [lowest] Fix pruning of machine histories -- Is this still an issue?
			// Explanation: Note, this is the full 'B' history, but it does not include
			// the 'pruned' histories or invalid parts of pruned histories.
			// Here: db-b-1 is completely invalid! In other cases, only parts of
			// an old history might be invalid!
				
			"C/(C1)/T=1",
			"C/(C2)/T=2",
			"C/(C3)/T=3",				
			"C/(C4)/T=5",
			"A/(A1,C4)/T=8",
			"A/(A2,C4)/T=9",
			"A/(A3,C4)/T=10",
			"A/(A3,B2,C4)/T=16",
			"A/(A3,B3,C4)/T=17",
			"A/(A3,B4,C4)/T=18", // db-b-4
			"A/(A3,B5,C4)/T=20", // db-b-5
			"B/(A3,B6,C4)/T=23", // db-b-6			
		}));
		
		// C
		allBranches.put("C", TestDatabaseUtil.createBranch(new String[] {
			"C/(C1)/T=1",
			"C/(C2)/T=2",
			"C/(C3)/T=3",				
			"C/(C4)/T=5",
			"A/(A1,C4)/T=8",
			"A/(A2,C4)/T=9",
			"A/(A3,C4)/T=10",	
			"A/(A4,C4)/T=11",
			"A/(A5,C4)/T=12", 
			"A/(A6,C4)/T=19", 			
			"C/(A6,C9)/T=22", 
		}));		

		/// Expected results ///
		TestResult expectedTestResult = new TestResult();
		
		expectedTestResult.lastCommonHeader = TestDatabaseUtil.createFromString("A/(A3,C4)/T=10");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"B", "A/(A3,B2,C4)/T=16", 
			"C", "A/(A4,C4)/T=11" 
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"C", "A/(A4,C4)/T=11" 
		});
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"C", "C/(A6,C9)/T=22"
		}).firstEntry();		
				
		/// Perform test ///
		testFromMachinePerspective(localMachineName, currentLocalVersion, allBranches, expectedTestResult);
	}				
	
	@Test
	public void testWinnersWinner() throws Exception {
		System.out.println("Winners winner test");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "B";
		DatabaseVersionHeader currentLocalVersion = TestDatabaseUtil.createFromString("A/(A1,C4)/T=8");
		DatabaseBranches allBranches = new DatabaseBranches();
		
		// A
		allBranches.put("A", TestDatabaseUtil.createBranch(new String[] {			
			"A/(A1,C4)/T=8",     // last common
			"A/(A2,C4)/T=9",     // first conflicting, wins
			
			"A/(A3,C4)/T=10",    // same as in B
			"A/(A4,C4)/T=11",    // second conflict, wins, winners winner
			"A/(A5,C4)/T=15"     // <<---- WINNERS WINNERS LAST DBV 
		}));
		
		// B
		allBranches.put("B", TestDatabaseUtil.createBranch(new String[] {
			"B/(A3,B1,C4)/T=12", // second conflict, loses = winners loser
			"B/(A3,B2,C4)/T=14"		
		}));
		
		// C
		allBranches.put("C", TestDatabaseUtil.createBranch(new String[] {
			"C/(C1)/T=1",
			"C/(C2)/T=2",
			"C/(C3)/T=3",		
			"C/(C4)/T=4",		
			"C/(A1,C5)/T=10", // first conflicting, loses
		}));		
				
		/// Expected results ///
		TestResult expectedTestResult = new TestResult();
		
		expectedTestResult.lastCommonHeader = TestDatabaseUtil.createFromString("A/(A1,C4)/T=8");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A2,C4)/T=9",
			"B", "A/(A2,C4)/T=9",
			"C", "C/(A1,C5)/T=10"			
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A2,C4)/T=9",
			"B", "A/(A2,C4)/T=9",
		});		
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A5,C4)/T=15"
		}).firstEntry();
				
		/// Perform test ///
		testFromMachinePerspective(localMachineName, currentLocalVersion, allBranches, expectedTestResult);
	}			
	
	@Test
	public void testWinningBranch() throws Exception {	
		System.out.println("Winners winner test");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "D";
		DatabaseVersionHeader currentLocalVersion = TestDatabaseUtil.createFromString("A/(A1,C4)/T=8");
		DatabaseBranches allBranches = new DatabaseBranches();
		
		// A
		allBranches.put("A", TestDatabaseUtil.createBranch(new String[] {
			"A/(A2,C4)/T=9",     // first conflicting, wins
			
			"A/(A3,C4)/T=10",    // same as in B
			"A/(A4,C4)/T=11",    // second conflict, wins, winners winner
			"A/(A5,B1,C4)/T=14"     // <<---- WINNERS WINNERS LAST DBV 
		}));
		
		// B
		allBranches.put("B", TestDatabaseUtil.createBranch(new String[] {
			"B/(A4,B1,C4)/T=12", // second conflict, loses = winners loser
			"B/(A4,B2,C4)/T=15"		
		}));
		
		// C
		allBranches.put("C", TestDatabaseUtil.createBranch(new String[] {	
			"C/(A1,C5)/T=10", // first conflicting, loses
		}));		
		
		allBranches.put("D", TestDatabaseUtil.createBranch(new String[] {	
				"C/(C1)/T=1",
				"C/(C2)/T=2",
				"C/(C3)/T=3",	
				"C/(C4)/T=4",	
				"A/(A1,C4)/T=8",     // last common - current DBV in B!
//				"A/(A2,C4)/T=9",     // new from here -> first conflicting, wins
//				
//				"A/(A3,C4)/T=10",    // same as in A
//				"B/(A4,B1,C4)/T=12", // second conflict, loses = winners loser
//				"B/(A4,B2,C4)/T=15"		
			}));		
				
		/// Expected results ///
		TestResult expectedTestResult = new TestResult();
		
		expectedTestResult.lastCommonHeader = TestDatabaseUtil.createFromString("A/(A1,C4)/T=8");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A2,C4)/T=9",
			"B", "A/(A2,C4)/T=9",
			"C", "C/(A1,C5)/T=10"			
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A2,C4)/T=9",
			"B", "A/(A2,C4)/T=9",
		});		
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A5,B1,C4)/T=14"
		}).firstEntry();
				
		/// Perform test ///
		testFromMachinePerspective(localMachineName, currentLocalVersion, allBranches, expectedTestResult);
	}		
	
	@Test
	public void testOneLocalVersionDetermineLastCommon() throws Exception {
		
		/* Scenario: The local machine ("C") has only one local database version
		 */
		
		/// Input data ///
		String localMachineName = "C";
		DatabaseVersionHeader currentLocalVersion = TestDatabaseUtil.createFromString("A/(A1)/T=1376074225169");
		DatabaseBranches allBranches = new DatabaseBranches();

		allBranches.put("A", TestDatabaseUtil.createBranch(new String[] {
			"A/(A1)/T=1376074225169",
			"A/(A2)/T=1376074225230",
		}));
		
		allBranches.put("C", TestDatabaseUtil.createBranch(new String[] {
			"A/(A1)/T=1376074225169",
		}));
				
		/// Expected results ///
		TestResult expectedTestResult = new TestResult();
		
		expectedTestResult.lastCommonHeader = TestDatabaseUtil.createFromString("A/(A1)/T=1376074225169");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A2)/T=1376074225230",
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A2)/T=1376074225230",
		});		
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A2)/T=1376074225230"
		}).firstEntry();
				
		/// Perform test ///
		testFromMachinePerspective(localMachineName, currentLocalVersion, allBranches, expectedTestResult);
	}			
	
	@Test
	public void testTwoWinningVersionsWithSameTimestamp() throws Exception {
		/* Scenario: Three clients, to conflicting DBVs with the same timestamp
		 *           --> A should win over B (alphabetical order)
		 */
		
		/// Input data ///
		String localMachineName = "C";
		DatabaseVersionHeader currentLocalVersion = TestDatabaseUtil.createFromString("A/(A2)/T=1376074225230");
		DatabaseBranches allBranches = new DatabaseBranches();

		allBranches.put("A", TestDatabaseUtil.createBranch(new String[] {
			"A/(A1)/T=1376074225169",
			"A/(A2)/T=1376074225230",
			"A/(A3)/T=1376074225256",

			// Conflicts with A -> A3,B1; also: SAME timestamp! 
			"A/(A4)/T=9999999999999",
		}));
		
		allBranches.put("B", TestDatabaseUtil.createBranch(new String[] {
			// Conflicts with B -> A4; also: SAME timestamp!
			"B/(A3,B1)/T=9999999999999"
		}));
		
		allBranches.put("C", TestDatabaseUtil.createBranch(new String[] {
			"A/(A1)/T=1376074225169",
			"A/(A2)/T=1376074225230",
		}));
				
		/// Expected results ///
		TestResult expectedTestResult = new TestResult();
		
		expectedTestResult.lastCommonHeader = TestDatabaseUtil.createFromString("A/(A2)/T=1376074225230");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A3)/T=1376074225256",
			"B", "A/(A3)/T=1376074225256",
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A3)/T=1376074225256",
			"B", "A/(A3)/T=1376074225256",
		});		
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A4)/T=9999999999999"
		}).firstEntry();
				
		/// Perform test ///
		testFromMachinePerspective(localMachineName, currentLocalVersion, allBranches, expectedTestResult);
	}			

	@Test
	public void testStitchBranches() throws Exception {
		DatabaseBranches allBranches = new DatabaseBranches(); 
		
		allBranches.put("A", TestDatabaseUtil.createBranch(new String[] {
			"A/(A1)/T=1376074225169",
			"A/(A2)/T=1376074225230",
			"A/(A3)/T=1376074225256",
		}));
		
		allBranches.put("B", TestDatabaseUtil.createBranch(new String[] {
			"B/(A3,B1)/T=1376074225356"
		}));
		
		allBranches.put("C", TestDatabaseUtil.createBranch(new String[] {
			"C/(C1)/T=1376074225383",
			"C/(C2)/T=1376074225399",
			"C/(C3)/T=1376074225416",
		}));		
		
		DatabaseReconciliator databaseVersionUpdateDetector = new DatabaseReconciliator();
		DatabaseBranches actualStitchedRemoteBranches = databaseVersionUpdateDetector.stitchBranches(allBranches, "D", new DatabaseBranch());
		
		DatabaseBranches expectedStitchedBranches = new DatabaseBranches();
		
		expectedStitchedBranches.put("A", TestDatabaseUtil.createBranch(new String[] {
			"A/(A1)/T=1376074225169",
			"A/(A2)/T=1376074225230",
			"A/(A3)/T=1376074225256",
		}));
		
		expectedStitchedBranches.put("B", TestDatabaseUtil.createBranch(new String[] {
			"A/(A1)/T=1376074225169",
			"A/(A2)/T=1376074225230",
			"A/(A3)/T=1376074225256",
			"B/(A3,B1)/T=1376074225356"
		}));
		
		expectedStitchedBranches.put("C", TestDatabaseUtil.createBranch(new String[] {
			"C/(C1)/T=1376074225383",
			"C/(C2)/T=1376074225399",
			"C/(C3)/T=1376074225416",
		}));
		
		assertEquals("Stitched branches not equal.", expectedStitchedBranches.toString(), actualStitchedRemoteBranches.toString());
	}
	
	@Test
	public void testStitchBranches2() throws Exception {		
		DatabaseBranches allBranches = new DatabaseBranches();
		
		allBranches.put("A", TestDatabaseUtil.createBranch(new String[] {
			"A/(A1)/T=1",
			"A/(A2)/T=2",
			// --> B 
			"A/(A3,B1)/T=4",
			"A/(A4,B1)/T=5",
			"A/(A5,B1)/T=6",
			// --> C
			
		}));
		
		allBranches.put("B", TestDatabaseUtil.createBranch(new String[] {
			"B/(A2,B1)/T=3",
			// --> A
			"B/(A5,B2,C2)/T=9",
			// --> C
		}));
		
		allBranches.put("C", TestDatabaseUtil.createBranch(new String[] {
			"C/(A5,B1,C1)/T=7",
			"C/(A5,B1,C2)/T=8",
			// --> B
			"C/(A5,B2,C3)/T=10",
		}));		
		
		DatabaseReconciliator databaseVersionUpdateDetector = new DatabaseReconciliator();
		DatabaseBranches actualStitchedRemoteBranches = databaseVersionUpdateDetector.stitchBranches(allBranches, "D", new DatabaseBranch());
		
		DatabaseBranches expectedStitchedBranches = new DatabaseBranches();
		
		expectedStitchedBranches.put("A", TestDatabaseUtil.createBranch(new String[] {
				"A/(A1)/T=1",
				"A/(A2)/T=2",
				"B/(A2,B1)/T=3",
				"A/(A3,B1)/T=4",
				"A/(A4,B1)/T=5",
				"A/(A5,B1)/T=6",
		}));
		
		expectedStitchedBranches.put("B", TestDatabaseUtil.createBranch(new String[] {
				"A/(A1)/T=1",
				"A/(A2)/T=2",
				"B/(A2,B1)/T=3",
				"A/(A3,B1)/T=4",
				"A/(A4,B1)/T=5",
				"A/(A5,B1)/T=6",
				"C/(A5,B1,C1)/T=7",
				"C/(A5,B1,C2)/T=8",
				"B/(A5,B2,C2)/T=9",
		}));
		
		expectedStitchedBranches.put("C", TestDatabaseUtil.createBranch(new String[] {
				"A/(A1)/T=1",
				"A/(A2)/T=2",
				"B/(A2,B1)/T=3",
				"A/(A3,B1)/T=4",
				"A/(A4,B1)/T=5",
				"A/(A5,B1)/T=6",
				"C/(A5,B1,C1)/T=7",
				"C/(A5,B1,C2)/T=8",
				"B/(A5,B2,C2)/T=9",
				"C/(A5,B2,C3)/T=10",
		}));
		
		assertEquals("Stitched branches not equal.", expectedStitchedBranches.toString(), actualStitchedRemoteBranches.toString());
	}		

	private void testFromMachinePerspective(String localMachineName, DatabaseVersionHeader currentLocalVersion, DatabaseBranches allBranches, TestResult expectedTestResult) throws Exception {
		// Print them all
		System.out.println("testFromMachinePerspective('"+localMachineName+"') with database version headers:");
		
		printBranches(allBranches);
			
	
		System.out.println("----------");
		
		DatabaseReconciliator databaseReconciliator = new DatabaseReconciliator();
		TestResult actualTestResult = new TestResult();
		
		// Get 'local' branch
		DatabaseBranch localBranch = allBranches.getBranch(localMachineName);
		
		// Get all the other ones (clone 'all', and remove local)
		DatabaseBranches unstitchedRemoteBranches = allBranches.clone();
		unstitchedRemoteBranches.remove(localMachineName);

		DatabaseBranches stitchedRemoteBranches = databaseReconciliator.stitchBranches(unstitchedRemoteBranches, localMachineName, localBranch);
		
		System.out.println("Before Orchestration : Remote");
		printBranches(unstitchedRemoteBranches);
		System.out.println("Before Orchestration : Local");
		printBranch(localBranch);
		System.out.println("BEGIN Branch Orchestration!! ----------");
		printBranches(stitchedRemoteBranches);				
		System.out.println("END Branch Orchestration ----------");
				
		actualTestResult.lastCommonHeader = databaseReconciliator.findLastCommonDatabaseVersionHeader(localBranch, stitchedRemoteBranches);
		actualTestResult.firstConflictingDatabaseVersionHeaders = databaseReconciliator.findFirstConflictingDatabaseVersionHeader(actualTestResult.lastCommonHeader, stitchedRemoteBranches);
		actualTestResult.winningFirstConflictingDatabaseVersionHeaders = databaseReconciliator.findWinningFirstConflictingDatabaseVersionHeaders(actualTestResult.firstConflictingDatabaseVersionHeaders);
		actualTestResult.winnersWinnersLastDatabaseVersionHeader = databaseReconciliator.findWinnersLastDatabaseVersionHeader(actualTestResult.winningFirstConflictingDatabaseVersionHeaders, stitchedRemoteBranches);
		
		System.out.println("Actual lastCommonDatabaseVersionHeader = " +actualTestResult.lastCommonHeader);
		System.out.println("Expect lastCommonDatabaseVersionHeader = " +expectedTestResult.lastCommonHeader);

		System.out.println("Actual firstConflictingDatabaseVersionHeaders = "); printMap(actualTestResult.firstConflictingDatabaseVersionHeaders);
		System.out.println("Expect firstConflictingDatabaseVersionHeaders = "); printMap(expectedTestResult.firstConflictingDatabaseVersionHeaders);

		System.out.println("Actual winningFirstConflictingDatabaseVersionHeaders = "); printMap(actualTestResult.winningFirstConflictingDatabaseVersionHeaders);
		System.out.println("Expect winningFirstConflictingDatabaseVersionHeaders = "); printMap(expectedTestResult.winningFirstConflictingDatabaseVersionHeaders);
		
		System.out.println("Actual winnersWinnersLastDatabaseVersionHeader = " + actualTestResult.winnersWinnersLastDatabaseVersionHeader);
		System.out.println("Expect winnersWinnersLastDatabaseVersionHeader = " + expectedTestResult.winnersWinnersLastDatabaseVersionHeader);

		assertEquals("Different last common database version header expected", expectedTestResult.lastCommonHeader, actualTestResult.lastCommonHeader);
		assertEquals("Different first conflicting versions expected", expectedTestResult.firstConflictingDatabaseVersionHeaders, actualTestResult.firstConflictingDatabaseVersionHeaders);
		assertEquals("Different winning first conflicting versions expected", expectedTestResult.winningFirstConflictingDatabaseVersionHeaders, actualTestResult.winningFirstConflictingDatabaseVersionHeaders);
		assertEquals("Different winners winners last version expected", expectedTestResult.winnersWinnersLastDatabaseVersionHeader, actualTestResult.winnersWinnersLastDatabaseVersionHeader);
	}

	private void printBranches(DatabaseBranches branches) {
		for (String machineName : branches.getClients()) {
			System.out.println(machineName+":");
			printBranch(branches.getBranch(machineName));
		}
	}
	
	private void printBranch(DatabaseBranch branch) {
		for (DatabaseVersionHeader databaseVersionHeader : branch.getAll()) {
			System.out.println("- "+databaseVersionHeader);
		}
	}

	private void printMap(Map<?, ?> someMap) {
		for (Map.Entry<?, ?> entry : someMap.entrySet()) {
			System.out.println("- "+entry.getKey()+": "+entry.getValue());
		}
	}		
	
	private class TestResult {
		DatabaseVersionHeader lastCommonHeader;		
		TreeMap<String, DatabaseVersionHeader> firstConflictingDatabaseVersionHeaders;
		TreeMap<String, DatabaseVersionHeader> winningFirstConflictingDatabaseVersionHeaders;
		Map.Entry<String, DatabaseVersionHeader> winnersWinnersLastDatabaseVersionHeader;	
	}
}
