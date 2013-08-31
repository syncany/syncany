package org.syncany.tests.database;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.syncany.database.Branch;
import org.syncany.database.Branches;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.operations.DatabaseReconciliator;
import org.syncany.tests.util.TestDatabaseVersionUtil;

/*
 * This test class uses tests one scenario using the DatabaseVersionUpdateDetector.
 * The algorithm used is described in the respective class ('algorithm B').
 *  
 * ////////////////////////////////////////////////////////////////////////////////////////////////////
 * 
 * TEST SCENARIO 
 * ----------------------------------------------------------------------------------------------------
 * T is the local timestamp, A/B/C are clients/machines, "-->" is a "sync up" operation,
 * "<--" is a "sync down" operation, "(CONFLICT X)" describes the comparison to be taken
 * and is described in detail below.
 * ----------------------------------------------------------------------------------------------------
 * T    A                       B                      C                        Remote Storage
 * ----------------------------------------------------------------------------------------------------
 *      
 * 01                                                  (  ,  ,C1)/T=01 -------> db-c-1
 * 02                                                  (  ,  ,C2)/T=02         
 * 03                                                  (  ,  ,C3)/T=03 -------> db-c-3
 * 04                           (  ,  ,C1)/T=01 <
 * 04                           (  ,  ,C2)/T=02 <
 * 04                           (  ,  ,C3)/T=03 <------------------------------ db-c-1, db-c-3
 * 05                                                  (  ,  ,C4)/T=05 -------> db-c-4
 * 06   (  ,  ,C1)/T=01 <
 * 06   (  ,  ,C2)/T=02 <
 * 06   (  ,  ,C3)/T=03 <
 * 06   (  ,  ,C4)/T=05 <------------------------------------------------------ db-c-1, db-c-3, db-c-4
 * 07                           (  ,B1,C3)/T=07 ------------------------------> db-b-1
 * 08   (A1,  ,C4)/T=08 
 * 09   (A2,  ,C4)/T=09 
 * 10   (A3,  ,C4)/T=10 ------------------------------------------------------> db-a-3
 * 
 *                              (CONFLICT 1) <--------------------------------- db-c-4, db-a-3
 *                              (A3,  ,C4)/T=10 wins
 *                              (A3,B2,C4)/T=.. after merge
 *                              To be uploaded
 *                        
 *      (CONFLICT 2) <--------------------------------------------------------- db-b-1
 *      (A3,  ,C4)/T=10 wins
 *      That's me. Do nothing.
 *      
 *                                                     (CONFLICT 3) <---------- db-a-3, db-b-1
 *                                                     (A3,  ,C4)/T=10 wins
 *                                                     To be applied locally
 *                              
 * 11   (A4,  ,C4)/T=11 
 * 12   (A5,  ,C4)/T=12 ------------------------------------------------------> db-a-5
 * 13                                                  (A3,  ,C5)/T=13 -------> db-c-5
 * 14                                                  (A3,  ,C6)/T=14         
 * 15                                                  (A3,  ,C7)/T=15 -------> db-c-7
 * 16                           (A3,B2,C4)/T=16                                        
 * 17                           (A3,B3,C4)/T=17 
 * 18                           (A3,B4,C4)/T=18 ------------------------------> db-b-4
 * 19   (A6,  ,C4)/T=19 ------------------------------------------------------> db-a-6
 * 20                           (A3,B5,C4)/T=20 ------------------------------> db-b-5
 * 21                                                  (A3,  ,C8)/T=21 -------> db-c-8
 * 
 *                              (CONFLICT 4) <--------------------------------- db-a-5, db-c-5, db-c-7, db-a-6, db-c-8
 *                              (A6,  ,C4)/T=19 wins
 *                              (A6,B6,C4)/T=.. after merge
 *                              To be uploaded
 * 
 *      (CONFLICT 5) <--------------------------------------------------------- db-c-5, db-c-7, db-b-4, db-b-5, db-c-8
 *      (A6,  ,C4)/T=10 wins
 *      That's me. Do nothing.
 *      
 *                                                     (CONFLICT 6) <---------- db-a-5, db-b-4, db-a-6, db-b-5
 *                                                     (A6,  ,C4)/T=19 wins
 *                                                     (A6,  ,C9)/T=.. after merge
 *                                                     To be uploaded	                                                     
 *                                                     
 * 22                                                  (A6,  ,C9)/T=22 -------> db-c-9
 * 23                           (A6,B6,C4)/T=23 ------------------------------> db-b-6
 * 
 *                                                     (CONFLICT 7) <---------- db-b-1, db-b-4, db-b-5, db-b-6, db-b-9
 *                                                     	                                                     
 *                              (CONFLICT 8) <--------------------------------- db-c-9
 *                              
 *      (CONFLICT 9) <--------------------------------------------------------- db-b-6, db-c-9
 *      
 * ////////////////////////////////////////////////////////////////////////////////////////////////////
 *      
 * CONFLICT 1 at B: 
 *  - Local:     (  ,  ,C1)/T=01
 *               (  ,  ,C2)/T=02
 *               (  ,  ,C3)/T=03     
 *               (  ,B1,C3)/T=07 (last B)
 *  - db-c-4:    (  ,  ,C1)/T=01
 *               (  ,  ,C2)/T=02
 *               (  ,  ,C3)/T=03
 *               (  ,  ,C4)/T=05 (last C)
 *  - db-a-3:    (  ,  ,C1)/T=01
 *               (  ,  ,C2)/T=02
 *               (  ,  ,C3)/T=03
 *               (  ,  ,C4)/T=05
 *               (A1,  ,C4)/T=08
 *               (A2,  ,C4)/T=09
 *               (A3,  ,C4)/T=10 (last A)
 *               
 *  - Last versions conflicts:                (  ,B1,C3)/T=07 || (  ,  ,C4)/T=05
 *                                            (  ,B1,C3)/T=07 || (A3,  ,C4)/T=10  
 *  - Last common version between clients:    (  ,  ,C3)/T=03
 *  - First conflicting version per client:   (  ,  ,C4)/T=05 (first conflicting A)
 *                                            (  ,B1,C3)/T=07 (first conflicting B)
 *                                            (  ,  ,C4)/T=05 (first conflicting C)
 *  - Winner of first conflicting versions:   (  ,  ,C4)/T=05
 *  - Winning clients histories:              A                  C
 *                                            (  ,  ,C4)/T=05    (  ,  ,C4)/T=05
 *                                            (A1,  ,C4)/T=08    (empty)
 *                                            (A2,  ,C4)/T=09
 *                                            (A3,  ,C4)/T=10
 *  - Conflicts in winning histories:         (empty)
 *  - Winning histories lengths               A                  C
 *                                            4                  1
 *  - Follow this history (ultimate winner):  A 
 *                                            
 *  - Result:
 *     Winner: (A3,  ,C4)/T=10 
 *     --> Conflicts with local version
 *     --> Local must merge local version (  ,B1,C3) in (A3,  ,C4)
 *     --> Local result is then (A3,B2,C4)		  
 *     
 * CONFLICT 2 at A: 
 *  - Local:     (  ,  ,C1)/T=01
 *               (  ,  ,C2)/T=02
 *               (  ,  ,C3)/T=03
 *               (A3,  ,C4)/T=10 (last A)
 *  - db-b-1:    (  ,  ,C1)/T=01
 *               (  ,  ,C2)/T=02
 *               (  ,  ,C3)/T=03
 *               (  ,B1,C3)/T=07 (last B)
 *               
 *  - Last versions conflicts:                (  ,B1,C3)/T=07 || (A3,  ,C4)/T=10
 *  - Last common version between clients:    (  ,  ,C3)/T=03 
 *  - First conflicting version per client:   (  ,  ,C4)/T=05 (first conflicting A)
 *                                            (  ,B1,C3)/T=07 (first conflicting B)
 *  - Winner of first conflicting versions:   (  ,  ,C4)/T=05
 *  - Winning clients histories:              A                   
 *                                            (  ,  ,C4)/T=05     
 *                                            (A1,  ,C4)/T=08    
 *                                            (A2,  ,C4)/T=09
 *                                            (A3,  ,C4)/T=10
 *  - Conflicts in winning histories:         (empty)
 *  - Winning histories lengths               4   
 *  - Follow this history (ultimate winner):  A 
 *                                            
 *  - Result:
 *     Winner: (A3,  ,C4)/T=10 
 *     --> That's me. Do nothing.     
 *     
 * CONFLICT 3 at C: 
 *  - Local:     (  ,  ,C1)/T=01
 *               (  ,  ,C2)/T=02
 *               (  ,  ,C3)/T=03
 *               (  ,  ,C4)/T=05 (last C)
 *  - db-b-1:    (  ,  ,C1)/T=01
 *               (  ,  ,C2)/T=02
 *               (  ,  ,C3)/T=03
 *               (  ,B1,C3)/T=07 (last B)
 *  - db-a-3:    (  ,  ,C1)/T=01
 *               (  ,  ,C2)/T=02
 *               (  ,  ,C3)/T=03
 *               (  ,  ,C4)/T=05
 *               (A1,  ,C4)/T=08
 *               (A2,  ,C4)/T=09
 *               (A3,  ,C4)/T=10 (last A)
 *               
 *  - Last versions conflicts:                (  ,B1,C3)/T=07 || (A3,  ,C4)/T=10
 *  - Last common version between clients:    (  ,  ,C3)/T=03
 *  - First conflicting version per client:   (  ,  ,C4)/T=05 (first conflicting A)
 *                                            (  ,B1,C3)/T=07 (first conflicting B)
 *                                            (  ,  ,C4)/T=05 (first conflicting C)
 *  - Winner of first conflicting versions:   (  ,  ,C4)/T=05
 *  - Winning clients histories:              A                  C
 *                                            (  ,  ,C4)/T=05    (  ,  ,C4)/T=05
 *                                            (A1,  ,C4)/T=08    (empty)
 *                                            (A2,  ,C4)/T=09
 *                                            (A3,  ,C4)/T=10
 *  - Conflicts in winning histories:         (empty)
 *  - Winning histories lengths               A                  C
 *                                            4                  1
 *  - Follow this history (ultimate winner):  A 
 *                                            
 *  - Result:
 *     Winner: (A3,  ,C4)/T=10 
 *     --> That's not me. Must apply changes locally.     
 *     
 * CONFLICT 4 at B: 
 *  - Local:     (  ,  ,C1)/T=01
 *               (  ,  ,C2)/T=02
 *               (  ,  ,C3)/T=03
 *               (  ,B1,C3)/T=07                            TODO This should be the first conflict, right?
 *               (A3,B2,C4)/T=16 (first conflict B)         TODO FIXME THIS IS WRONG, RIGHT?
 *               (A3,B3,C4)/T=17
 *               (A3,B4,C4)/T=18
 *               (A3,B5,C4)/T=20 (last B)
 *  - db-a-5:    (A4,  ,C4)/T=11 (first conflict A)
 *               (A5,  ,C4)/T=12
 *  - db-a-6:    (A6,  ,C4)/T=19 (last A)
 *  - db-c-5:    (A3,  ,C5)/T=13 (first conflict C)
 *  - db-c-7:    (A3,  ,C6)/T=14
 *               (A3,  ,C7)/T=15
 *  - db-c-8:    (A3,  ,C8)/T=21 (last C)
 *               
 *  - Last versions conflicts:                (A3,B5,C4)/T=20 || (A6,  ,C4)/T=19
 *                                            (A3,B5,C4)/T=20 || (A3,  ,C8)/T=21
 *                                            (A6,  ,C4)/T=19 || (A3,  ,C8)/T=21 (TODO Do I care about the conflicts of other clients?)
 *  - Last common version between clients:    (empty)
 *  - First conflicting version per client:   (A4,  ,C4)/T=11 (first conflicting A)
 *                                            (A3,B2,C4)/T=16 (first conflicting B)
 *                                            (A3,  ,C5)/T=13 (first conflicting C)
 *  - Winner of first conflicting versions:   (A4,  ,C4)/T=11
 *  - Winning clients histories:              A               
 *                                            (A4,  ,C4)/T=11 
 *                                            (A5,  ,C4)/T=12 
 *                                            (A6,  ,C4)/T=19
 *  - Conflicts in winning histories:         (empty)
 *  - Winning histories lengths               (not relevant)
 *  - Follow this history (ultimate winner):  A 
 *                                            
 *  - Result:
 *     Winner: (A6,  ,C4)/T=19 
 *     --> Conflicts with local version
 *     --> Local must merge local version (A3,B5,C4) in (A6,  ,C4)
 *     --> Local result is then (A6,B6,C4)	         
 *     
 * CONFLICT 5 at A: 
 *  - Local:     (  ,  ,C1)/T=01
 *               (  ,  ,C2)/T=02
 *               (  ,  ,C3)/T=03
 *               (  ,  ,C4)/T=05
 *               (A4,  ,C4)/T=11 (first conflict A)
 *               (A5,  ,C4)/T=12
 *               (A6,  ,C4)/T=19 (last A)
 *  - db-b-4:    (A3,B2,C4)/T=16 (first conflict B)
 *               (A3,B3,C4)/T=17
 *               (A3,B4,C4)/T=18
 *  - db-b-5:    (A3,B5,C4)/T=20 (last B)
 *  - db-c-5:    (A3,  ,C5)/T=13 (first conflict C)
 *  - db-c-7:    (A3,  ,C6)/T=14
 *               (A3,  ,C7)/T=15
 *  - db-c-8:    (A3,  ,C8)/T=21 (last C)
 *               
 *  - Last versions conflicts:                (A3,B5,C4)/T=20 || (A6,  ,C4)/T=19
 *                                            (A3,B5,C4)/T=20 || (A3,  ,C8)/T=21 (TODO Do I care about the conflicts of other clients?)
 *                                            (A6,  ,C4)/T=19 || (A3,  ,C8)/T=21 
 *  - Last common version between clients:    (empty)
 *  - First conflicting version per client:   (A4,  ,C4)/T=11 (first conflicting A)
 *                                            (A3,B2,C4)/T=16 (first conflicting B)
 *                                            (A3,  ,C5)/T=13 (first conflicting C)
 *  - Winner of first conflicting versions:   (A4,  ,C4)/T=11
 *  - Winning clients histories:              A               
 *                                            (A4,  ,C4)/T=11 
 *                                            (A5,  ,C4)/T=12 
 *                                            (A6,  ,C4)/T=19
 *  - Conflicts in winning histories:         (empty)
 *  - Winning histories lengths               (not relevant)
 *  - Follow this history (ultimate winner):  A 
 *                                            
 *  - Result:
 *     Winner: (A6,  ,C4)/T=19 
 *     --> That's me. Do nothing.           
 *     
 * CONFLICT 6 at C: 
 *  - Local:     (  ,  ,C1)/T=01
 *               (  ,  ,C2)/T=02
 *               (  ,  ,C3)/T=03
 *               (  ,  ,C4)/T=05
 *               (A3,  ,C5)/T=13 (first conflict C)
 *               (A3,  ,C6)/T=14
 *               (A3,  ,C7)/T=15
 *               (A3,  ,C8)/T=21 (last C)
 *  - db-a-5:    (A4,  ,C4)/T=11 (first conflict A)
 *               (A5,  ,C4)/T=12
 *  - db-a-6:    (A6,  ,C4)/T=19 (last A)
 *  - db-b-4:    (A3,B2,C4)/T=16 (first conflict B)
 *               (A3,B3,C4)/T=17
 *               (A3,B4,C4)/T=18
 *  - db-b-5:    (A3,B5,C4)/T=20 (last B)
 *               
 *  - Last versions conflicts:                (A3,B5,C4)/T=20 || (A6,  ,C4)/T=19 (TODO Do I care about the conflicts of other clients?)
 *                                            (A3,B5,C4)/T=20 || (A3,  ,C8)/T=21 
 *                                            (A6,  ,C4)/T=19 || (A3,  ,C8)/T=21 
 *  - Last common version between clients:    (empty)
 *  - First conflicting version per client:   (A4,  ,C4)/T=11 (first conflicting A)
 *                                            (A3,B2,C4)/T=16 (first conflicting B)
 *                                            (A3,  ,C5)/T=13 (first conflicting C)
 *  - Winner of first conflicting versions:   (A4,  ,C4)/T=11
 *  - Winning clients histories:              A               
 *                                            (A4,  ,C4)/T=11 
 *                                            (A5,  ,C4)/T=12 
 *                                            (A6,  ,C4)/T=19
 *  - Conflicts in winning histories:         (empty)
 *  - Winning histories lengths               (not relevant)
 *  - Follow this history (ultimate winner):  A 
 *                                            
 *  - Result:
 *     Winner: (A6,  ,C4)/T=19 
 *     --> Conflicts with local version
 *     --> Local must merge local version (A3,  ,C8) in (A6,  ,C4)
 *     --> Local result is then (A6,  ,C9)	        
 *     
 * CONFLICT 7 at C: 
 *  - Local:     (  ,  ,C1)/T=01
 *               (  ,  ,C2)/T=02
 *               (  ,  ,C3)/T=03
 *               (  ,  ,C4)/T=05 
 *               (A3,  ,C5)/T=13 
 *               (A3,  ,C6)/T=14
 *               (A3,  ,C7)/T=15
 *               (A3,  ,C8)/T=21
 *               (A3,  ,C9)/T=22 (last C) 
 *  - db-b-1:    (  ,  ,C1)/T=01
 *               (  ,  ,C2)/T=02
 *               (  ,  ,C3)/T=03
 *               (  ,B1,C3)/T=07 
 *  - db-b-4:    (A3,B2,C4)/T=16 (first conflict B)
 *               (A3,B3,C4)/T=17
 *               (A3,B4,C4)/T=18
 *  - db-b-5:    (A3,B5,C4)/T=20 
 *  - db-b-6:    (A6,B6,C4)/T=23 (last B) 
 *  
 *  
 *  
 *  
 *  
 *               
 *  - Last versions conflicts:                (A6,B5,C4)/T=23 || (A3,  ,C9)/T=22 
 *  - Last common version between clients:    (empty)
 *  - First conflicting version per client:   (A3,  ,C9)/T=22 (first conflicting C)
 *                                            (A6,B6,C4)/T=23 (first conflicting B)
 *  - Winner of first conflicting versions:   (A4,  ,C4)/T=11
 *  - Winning clients histories:              A               
 *                                            (A4,  ,C4)/T=11 
 *                                            (A5,  ,C4)/T=12 
 *                                            (A6,  ,C4)/T=19
 *  - Conflicts in winning histories:         (empty)
 *  - Winning histories lengths               (not relevant)
 *  - Follow this history (ultimate winner):  A 
 *                                            
 *  - Result:
 *     Winner: (A6,  ,C4)/T=19 
 *     --> Conflicts with local version
 *     --> Local must merge local version (A3,  ,C8) in (A6,  ,C4)
 *     --> Local result is then (A6,  ,C9)	        
 */		
public class DatabaseReconciliatorTest {	
	@Test
	public void testUpdateDetectorConflict1AtMachineB() throws Exception {	
		System.out.println("CONFLICT 1");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "B";
		DatabaseVersionHeader currentLocalVersion = null;
		Branches allBranches = new Branches();
		 
		// A
		allBranches.add("A", TestDatabaseVersionUtil.createBranch(new String[] {
			"A/(A1,C4)/T=8/C",
			"A/(A2,C4)/T=9/A",
			"A/(A3,C4)/T=10/A"								
		}));
		
		// B
		allBranches.add("B", TestDatabaseVersionUtil.createBranch(new String[] {
			"C/(C1)/T=1",
			"C/(C2)/T=2/C",
			"C/(C3)/T=3/C",				
			"B/(B1,C3)/T=7/C"								
		}));
		
		// C
		allBranches.add("C", TestDatabaseVersionUtil.createBranch(new String[] {
			"C/(C4)/T=5/C"
		}));		
				
		/// Expected results ///
		TestResult expectedTestResult = new TestResult();
		
		expectedTestResult.lastCommonHeader = TestDatabaseVersionUtil.createFromString("C/(C3)/T=3");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A1,C4)/T=8/C",
			"B", "B/(B1,C3)/T=7/C",
			"C", "C/(C4)/T=5/C"			
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A1,C4)/T=8/C"
		});
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A3,C4)/T=10/C"
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
		//FIXME
		DatabaseVersionHeader currentLocalVersion = null;
		Branches allBranches = new Branches();
		
		// A
		allBranches.add("A", TestDatabaseVersionUtil.createBranch(new String[] {
			"C/(C1)/T=1",
			"C/(C2)/T=2/C",
			"C/(C3)/T=3/C",
			"C/(C4)/T=5/C",
			"A/(A1,C4)/T=8/C",
			"A/(A2,C4)/T=9/A",
			"A/(A3,C4)/T=10/A"								
		}));
		
		// B
		allBranches.add("B", TestDatabaseVersionUtil.createBranch(new String[] {
			"B/(B1,C3)/T=7"								
		}));
				
		/// Expected results ///
		TestResult expectedTestResult = new TestResult();
		
		expectedTestResult.lastCommonHeader = TestDatabaseVersionUtil.createFromString("C/(C3)/T=3");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "C/(C4)/T=5",
			"B", "B/(B1,C3)/T=7"
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "C/(C4)/T=5"
		});
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
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
		//FIXME
		DatabaseVersionHeader currentLocalVersion = null;
		Branches allBranches = new Branches();
		
		// A
		allBranches.add("A", TestDatabaseVersionUtil.createBranch(new String[] {
			"A/(A1,C4)/T=8",
			"A/(A2,C4)/T=9",
			"A/(A3,C4)/T=10"								
		}));
		
		// B
		allBranches.add("B", TestDatabaseVersionUtil.createBranch(new String[] {			
			"B/(B1,C3)/T=7"								
		}));
		
		// C
		allBranches.add("C", TestDatabaseVersionUtil.createBranch(new String[] {
			"C/(C1)/T=1",
			"C/(C2)/T=2",
			"C/(C3)/T=3",				
			"C/(C4)/T=5"
		}));		

		/// Expected results ///x
		TestResult expectedTestResult = new TestResult();
		
		expectedTestResult.lastCommonHeader = TestDatabaseVersionUtil.createFromString("C/(C3)/T=3");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A1,C4)/T=8",
			"B", "B/(B1,C3)/T=7",
			"C", "C/(C4)/T=5"
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A1,C4)/T=8"
		});
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
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
		//FIXME
		DatabaseVersionHeader currentLocalVersion = null;
		Branches allBranches = new Branches();
		
		// A
		allBranches.add("A", TestDatabaseVersionUtil.createBranch(new String[] {
			"A/(A4,C4)/T=11",
			"A/(A5,C4)/T=12", // db-a-5
			"A/(A6,C4)/T=19", // db-a-6
		}));
		
		// B
		allBranches.add("B", TestDatabaseVersionUtil.createBranch(new String[] {			
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
		allBranches.add("C", TestDatabaseVersionUtil.createBranch(new String[] {
			"C/(A3,C5)/T=13", // db-c-5
			"C/(A3,C6)/T=14",
			"C/(A3,C7)/T=15", // db-c-7
			"C/(A3,C8)/T=21", // db-c-8
		}));		

		/// Expected results ///
		TestResult expectedTestResult = new TestResult();
		
		expectedTestResult.lastCommonHeader = TestDatabaseVersionUtil.createFromString("A/(A3,C4)/T=10");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A4,C4)/T=11",
			"B", "B/(A3,B2,C4)/T=16",
			"C", "C/(A3,C5)/T=13"
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A4,C4)/T=11"
		});
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
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
		//FIXME
		DatabaseVersionHeader currentLocalVersion = null;
		Branches allBranches = new Branches();
		
		// A
		allBranches.add("A", TestDatabaseVersionUtil.createBranch(new String[] {
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
		allBranches.add("B", TestDatabaseVersionUtil.createBranch(new String[] {			
			"B/(A3,B2,C4)/T=16",
			"B/(A3,B3,C4)/T=17",
			"B/(A3,B4,C4)/T=18", // db-b-4
			"B/(A3,B5,C4)/T=20", // db-b-5
		}));
		
		// C
		allBranches.add("C", TestDatabaseVersionUtil.createBranch(new String[] {
			"C/(A3,C5)/T=13", // db-c-5
			"C/(A3,C6)/T=14",
			"C/(A3,C7)/T=15", // db-c-7
			"C/(A3,C8)/T=21", // db-c-8
		}));		

		/// Expected results ///
		TestResult expectedTestResult = new TestResult();
		
		expectedTestResult.lastCommonHeader = TestDatabaseVersionUtil.createFromString("A/(A3,C4)/T=10");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A4,C4)/T=11",
			"B", "B/(A3,B2,C4)/T=16",
			"C", "C/(A3,C5)/T=13"
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A4,C4)/T=11"
		});
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
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
		//FIXME
		DatabaseVersionHeader currentLocalVersion = null;
		Branches allBranches = new Branches();
		
		// A
		allBranches.add("A", TestDatabaseVersionUtil.createBranch(new String[] {
			"A/(A4,C4)/T=11",
			"A/(A5,C4)/T=12", // db-a-5
			"A/(A6,C4)/T=19", // db-a-6
		}));
		
		// B
		allBranches.add("B", TestDatabaseVersionUtil.createBranch(new String[] {			
			"B/(A3,B2,C4)/T=16",
			"B/(A3,B3,C4)/T=17",
			"B/(A3,B4,C4)/T=18", // db-b-4
			"B/(A3,B5,C4)/T=20", // db-b-5
		}));
		
		// C
		allBranches.add("C", TestDatabaseVersionUtil.createBranch(new String[] {
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
		
		expectedTestResult.lastCommonHeader = TestDatabaseVersionUtil.createFromString("A/(A3,C4)/T=10");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A4,C4)/T=11",
			"B", "B/(A3,B2,C4)/T=16",
			"C", "C/(A3,C5)/T=13"
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A4,C4)/T=11"
		});
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
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
		//FIXME
		DatabaseVersionHeader currentLocalVersion = null;

		Branches allBranches = new Branches();
		
		// B
		allBranches.add("B", TestDatabaseVersionUtil.createBranch(new String[] {
			// TODO Fix pruning of machine histories
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
		allBranches.add("C", TestDatabaseVersionUtil.createBranch(new String[] {
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
		
		expectedTestResult.lastCommonHeader = TestDatabaseVersionUtil.createFromString("A/(A3,C4)/T=10");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"B", "A/(A3,B2,C4)/T=16", 
			"C", "A/(A4,C4)/T=11" 
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"C", "A/(A4,C4)/T=11" 
		});
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"C", "C/(A6,C9)/T=22"
		}).firstEntry();		
				
		/// Perform test ///
		testFromMachinePerspective(localMachineName, currentLocalVersion, allBranches, expectedTestResult);
	}				
	
	@Test
	public void testWinnersWinner() throws Exception {	 // TODO Extract this from this class, not related to the scenario
		System.out.println("Winners winner test");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "B";
		DatabaseVersionHeader currentLocalVersion = TestDatabaseVersionUtil.createFromString("A/(A1,C4)/T=8");
		Branches allBranches = new Branches();
		
		// A
		allBranches.add("A", TestDatabaseVersionUtil.createBranch(new String[] {
			"C/(C1)/T=1",
			"C/(C2)/T=2",
			"C/(C3)/T=3",				
			"A/(A1,C4)/T=8",     // last common
			"A/(A2,C4)/T=9",     // first conflicting, wins
			
			"A/(A3,C4)/T=10",    // same as in B
			"A/(A4,C4)/T=11",    // second conflict, wins, winners winner
			"A/(A5,C4)/T=15"     // <<---- WINNERS WINNERS LAST DBV 
		}));
		
		// B
		allBranches.add("B", TestDatabaseVersionUtil.createBranch(new String[] {
			"C/(C1)/T=1",
			"C/(C2)/T=2",
			"C/(C3)/T=3",	
			"A/(A1,C4)/T=8",     // last common
			"A/(A2,C4)/T=9",     // first conflicting, wins
			
			"A/(A3,C4)/T=10",    // same as in A
			"B/(A3,B1,C5)/T=12", // second conflict, loses = winners loser
			"B/(A3,B2,C5)/T=14"		
		}));
		
		// C
		allBranches.add("C", TestDatabaseVersionUtil.createBranch(new String[] {
			"C/(C1)/T=1",
			"C/(C2)/T=2",
			"C/(C3)/T=3",	
			"A/(A1,C4)/T=8", // last common
			
			"C/(A1,C5)/T=10", // first conflicting, loses
		}));		
				
		/// Expected results ///
		TestResult expectedTestResult = new TestResult();
		
		expectedTestResult.lastCommonHeader = TestDatabaseVersionUtil.createFromString("A/(A1,C4)/T=8");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A2,C4)/T=9",
			"B", "A/(A2,C4)/T=9",
			"C", "C/(A1,C5)/T=10"			
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A2,C4)/T=9",
			"B", "A/(A2,C4)/T=9",
		});		
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A5,C4)/T=15"
		}).firstEntry();
				
		/// Perform test ///
		testFromMachinePerspective(localMachineName, currentLocalVersion, allBranches, expectedTestResult);
	}			
	
	@Test
	public void testWinningBranch() throws Exception {	 // TODO Extract this from this class, not related to the scenario
		System.out.println("Winners winner test");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "D";
		DatabaseVersionHeader currentLocalVersion = TestDatabaseVersionUtil.createFromString("A/(A1,C4)/T=8");
		Branches allBranches = new Branches();
		
		// A
		allBranches.add("A", TestDatabaseVersionUtil.createBranch(new String[] {
			"A/(A2,C4)/T=9",     // first conflicting, wins
			
			"A/(A3,C4)/T=10",    // same as in B
			"A/(A4,C4)/T=11",    // second conflict, wins, winners winner
			"A/(A5,B1,C4)/T=14"     // <<---- WINNERS WINNERS LAST DBV 
		}));
		
		// B
		allBranches.add("B", TestDatabaseVersionUtil.createBranch(new String[] {
			"B/(A4,B1,C4)/T=12", // second conflict, loses = winners loser
			"B/(A4,B2,C4)/T=15"		
		}));
		
		// C
		allBranches.add("C", TestDatabaseVersionUtil.createBranch(new String[] {	
			"C/(A1,C5)/T=10", // first conflicting, loses
		}));		
		
		allBranches.add("D", TestDatabaseVersionUtil.createBranch(new String[] {	
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
		
		expectedTestResult.lastCommonHeader = TestDatabaseVersionUtil.createFromString("A/(A1,C4)/T=8");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A2,C4)/T=9",
			"B", "A/(A2,C4)/T=9",
			"C", "C/(A1,C5)/T=10"			
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A2,C4)/T=9",
			"B", "A/(A2,C4)/T=9",
		});		
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A5,B1,C4)/T=14"
		}).firstEntry();
				
		/// Perform test ///
		testFromMachinePerspective(localMachineName, currentLocalVersion, allBranches, expectedTestResult);
	}			

	@Test
	public void testStitchBranches() throws Exception {
		Branches allBranches = new Branches(); 
		
		allBranches.add("A", TestDatabaseVersionUtil.createBranch(new String[] {
			"A/(A1)/T=1376074225169",
			"A/(A2)/T=1376074225230/A",
			"A/(A3)/T=1376074225256/A",
		}));
		
		allBranches.add("B", TestDatabaseVersionUtil.createBranch(new String[] {
			"B/(A3,B1)/T=1376074225356/A"
		}));
		
		allBranches.add("C", TestDatabaseVersionUtil.createBranch(new String[] {
			"C/(C1)/T=1376074225383",
			"C/(C2)/T=1376074225399/C",
			"C/(C3)/T=1376074225416/C",
		}));		
		
		DatabaseReconciliator databaseVersionUpdateDetector = new DatabaseReconciliator();
		Branches actualStitchedRemoteBranches = databaseVersionUpdateDetector.stitchRemoteBranches(allBranches, "D", new Branch());
		
		Branches expectedStitchedBranches = new Branches();
		
		expectedStitchedBranches.add("A", TestDatabaseVersionUtil.createBranch(new String[] {
			"A/(A1)/T=1376074225169",
			"A/(A2)/T=1376074225230",
			"A/(A3)/T=1376074225256",
		}));
		
		expectedStitchedBranches.add("B", TestDatabaseVersionUtil.createBranch(new String[] {
			"A/(A1)/T=1376074225169",
			"A/(A2)/T=1376074225230",
			"A/(A3)/T=1376074225256",
			"B/(A3,B1)/T=1376074225356"
		}));
		
		expectedStitchedBranches.add("B", TestDatabaseVersionUtil.createBranch(new String[] {
			"C/(C1)/T=1376074225383",
			"C/(C2)/T=1376074225399/C",
			"C/(C3)/T=1376074225416/C",
		}));
		
		assertEquals("Stitched branches not equal.", expectedStitchedBranches.toString(), actualStitchedRemoteBranches.toString());
	}
	
	@Test
	public void testStitchBranches2() throws Exception {		
		Branches allBranches = new Branches();
		
		allBranches.add("A", TestDatabaseVersionUtil.createBranch(new String[] {
			"A/(A1)/T=1",
			"A/(A2)/T=2/A",
			// --> B 
			"A/(A3,B1)/T=4/B",
			"A/(A4,B1)/T=5/A",
			"A/(A5,B1)/T=6/A",
			// --> C
			
		}));
		
		allBranches.add("B", TestDatabaseVersionUtil.createBranch(new String[] {
			"B/(A2,B1)/T=3/A",
			// --> A
			"B/(A5,B2,C2)/T=9/C",
			// --> C
		}));
		
		allBranches.add("C", TestDatabaseVersionUtil.createBranch(new String[] {
			"C/(A5,B1,C1)/T=7/A",
			"C/(A5,B1,C2)/T=8/C",
			// --> B
			"C/(A5,B2,C3)/T=10/B",
		}));		
		
		DatabaseReconciliator databaseVersionUpdateDetector = new DatabaseReconciliator();
		Branches actualStitchedRemoteBranches = databaseVersionUpdateDetector.stitchRemoteBranches(allBranches, "D", new Branch());
		
		Branches expectedStitchedBranches = new Branches();
		
		expectedStitchedBranches.add("A", TestDatabaseVersionUtil.createBranch(new String[] {
			"A/(A1)/T=1376074225169",
			"A/(A2)/T=1376074225230",
			"A/(A3)/T=1376074225256",
		}));
		
		expectedStitchedBranches.add("B", TestDatabaseVersionUtil.createBranch(new String[] {
			"A/(A1)/T=1376074225169",
			"A/(A2)/T=1376074225230",
			"A/(A3)/T=1376074225256",
			"B/(A3,B1)/T=1376074225356"
		}));
		
		expectedStitchedBranches.add("B", TestDatabaseVersionUtil.createBranch(new String[] {
			"C/(C1)/T=1376074225383",
			"C/(C2)/T=1376074225399/C",
			"C/(C3)/T=1376074225416/C",
		}));
		
		assertEquals("Stitched branches not equal.", expectedStitchedBranches.toString(), actualStitchedRemoteBranches.toString());
	}	

	private void testFromMachinePerspective(String localMachineName, DatabaseVersionHeader currentLocalVersion, Branches allBranches, TestResult expectedTestResult) throws Exception {
		// Print them all
		System.out.println("testFromMachinePerspective('"+localMachineName+"') with database version headers:");
		
		printBranches(allBranches);
			
	
		System.out.println("----------");
		
		DatabaseReconciliator databaseReconciliator = new DatabaseReconciliator();
		TestResult actualTestResult = new TestResult();
		
		// Get 'local' branch
		Branch localBranch = allBranches.getBranch(localMachineName);
		
		// Get all the other ones (clone 'all', and remove local)
		Branches unstitchedRemoteBranches = allBranches.clone();
		unstitchedRemoteBranches.remove(localMachineName);


		Branches stitchedRemoteBranches = databaseReconciliator.stitchRemoteBranches(unstitchedRemoteBranches, localMachineName, localBranch);

		Branches allStitchedBranches = stitchedRemoteBranches.clone();
		allStitchedBranches.add(localMachineName, localBranch);
		
		System.out.println("Before Orchestration : Remote");
		printBranches(unstitchedRemoteBranches);
		System.out.println("Before Orchestration : Local");
		printBranch(localBranch);
		System.out.println("BEGIN Branch Orchestration!! ----------");
		printBranches(stitchedRemoteBranches);				
		System.out.println("END Branch Orchestration ----------");
				
		actualTestResult.lastCommonHeader = databaseReconciliator.findLastCommonDatabaseVersionHeader(localBranch, allStitchedBranches);
		actualTestResult.firstConflictingDatabaseVersionHeaders = databaseReconciliator.findFirstConflictingDatabaseVersionHeader(actualTestResult.lastCommonHeader, allStitchedBranches);
		actualTestResult.winningFirstConflictingDatabaseVersionHeaders = databaseReconciliator.findWinningFirstConflictingDatabaseVersionHeaders(actualTestResult.firstConflictingDatabaseVersionHeaders);
		actualTestResult.winnersWinnersLastDatabaseVersionHeader = databaseReconciliator.findWinnersWinnersLastDatabaseVersionHeader(actualTestResult.winningFirstConflictingDatabaseVersionHeaders, allStitchedBranches);
		
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

	private void printBranches(Branches branches) {
		for (String machineName : branches.getClients()) {
			System.out.println(machineName+":");
			printBranch(branches.getBranch(machineName));
		}
	}
	
	private void printBranch(Branch branch) {
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
