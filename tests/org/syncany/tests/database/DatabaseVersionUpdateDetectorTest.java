package org.syncany.tests.database;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.operations.DatabaseVersionUpdateDetector;
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
public class DatabaseVersionUpdateDetectorTest {	
	@Test
	public void testUpdateDetectorConflict1AtMachineB() throws Exception {	
		System.out.println("CONFLICT 1");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "B";
		TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> allDatabaseVersionHeaders = new TreeMap<String, TreeMap<Long, DatabaseVersionHeader>>();
		
		// A
		allDatabaseVersionHeaders.put("A", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {
			"A/(A1,C4)/T=8",
			"A/(A2,C4)/T=9",
			"A/(A3,C4)/T=10"								
		}));
		
		// B
		allDatabaseVersionHeaders.put("B", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {
			"C/(C1)/T=1",
			"C/(C2)/T=2",
			"C/(C3)/T=3",				
			"B/(B1,C3)/T=7"								
		}));
		
		// C
		allDatabaseVersionHeaders.put("C", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {
			"C/(C4)/T=5"
		}));		
				
		/// Expected results ///
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
		testFromMachinePerspective(localMachineName, allDatabaseVersionHeaders, expectedTestResult);
	}			
	
	@Test
	public void testUpdateDetectorConflict2AtMachineA() throws Exception {	
		System.out.println("CONFLICT 2");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "A";
		TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> allDatabaseVersionHeaders = new TreeMap<String, TreeMap<Long, DatabaseVersionHeader>>();
		
		// A
		allDatabaseVersionHeaders.put("A", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {
			"C/(C1)/T=1",
			"C/(C2)/T=2",
			"C/(C3)/T=3",
			"C/(C4)/T=5",
			"A/(A1,C4)/T=8",
			"A/(A2,C4)/T=9",
			"A/(A3,C4)/T=10"								
		}));
		
		// B
		allDatabaseVersionHeaders.put("B", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {
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
		testFromMachinePerspective(localMachineName, allDatabaseVersionHeaders, expectedTestResult);
	}	
	
	@Test
	public void testUpdateDetectorConflict3AtMachineC() throws Exception {	
		System.out.println("CONFLICT 3");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "C";
		TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> allDatabaseVersionHeaders = new TreeMap<String, TreeMap<Long, DatabaseVersionHeader>>();
		
		// A
		allDatabaseVersionHeaders.put("A", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {
			"A/(A1,C4)/T=8",
			"A/(A2,C4)/T=9",
			"A/(A3,C4)/T=10"								
		}));
		
		// B
		allDatabaseVersionHeaders.put("B", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {			
			"B/(B1,C3)/T=7"								
		}));
		
		// C
		allDatabaseVersionHeaders.put("C", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {
			"C/(C1)/T=1",
			"C/(C2)/T=2",
			"C/(C3)/T=3",				
			"C/(C4)/T=5"
		}));		

		/// Expected results ///
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
		testFromMachinePerspective(localMachineName, allDatabaseVersionHeaders, expectedTestResult);
	}			
	
	@Test
	public void testUpdateDetectorConflict4AtMachineB() throws Exception {	
		System.out.println("CONFLICT 4");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "B";
		TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> allDatabaseVersionHeaders = new TreeMap<String, TreeMap<Long, DatabaseVersionHeader>>();
		
		// A
		allDatabaseVersionHeaders.put("A", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {
			"A/(A4,C4)/T=11",
			"A/(A5,C4)/T=12", // db-a-5
			"A/(A6,C4)/T=19", // db-a-6
		}));
		
		// B
		allDatabaseVersionHeaders.put("B", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {			
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
		allDatabaseVersionHeaders.put("C", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {
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
		testFromMachinePerspective(localMachineName, allDatabaseVersionHeaders, expectedTestResult);
	}			
	
	@Test
	public void testUpdateDetectorConflict5AtMachineA() throws Exception {	
		System.out.println("CONFLICT 5");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "A";
		TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> allDatabaseVersionHeaders = new TreeMap<String, TreeMap<Long, DatabaseVersionHeader>>();
		
		// A
		allDatabaseVersionHeaders.put("A", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {
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
		allDatabaseVersionHeaders.put("B", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {			
			"B/(A3,B2,C4)/T=16",
			"B/(A3,B3,C4)/T=17",
			"B/(A3,B4,C4)/T=18", // db-b-4
			"B/(A3,B5,C4)/T=20", // db-b-5
		}));
		
		// C
		allDatabaseVersionHeaders.put("C", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {
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
		testFromMachinePerspective(localMachineName, allDatabaseVersionHeaders, expectedTestResult);
	}				
	
	@Test
	public void testUpdateDetectorConflict6AtMachineC() throws Exception {	
		System.out.println("CONFLICT 6");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "C";
		TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> allDatabaseVersionHeaders = new TreeMap<String, TreeMap<Long, DatabaseVersionHeader>>();
		
		// A
		allDatabaseVersionHeaders.put("A", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {
			"A/(A4,C4)/T=11",
			"A/(A5,C4)/T=12", // db-a-5
			"A/(A6,C4)/T=19", // db-a-6
		}));
		
		// B
		allDatabaseVersionHeaders.put("B", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {			
			"B/(A3,B2,C4)/T=16",
			"B/(A3,B3,C4)/T=17",
			"B/(A3,B4,C4)/T=18", // db-b-4
			"B/(A3,B5,C4)/T=20", // db-b-5
		}));
		
		// C
		allDatabaseVersionHeaders.put("C", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {
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
		testFromMachinePerspective(localMachineName, allDatabaseVersionHeaders, expectedTestResult);
	}				
	
	@Test
	public void testUpdateDetectorConflict7AtMachineC() throws Exception {	
		System.out.println("CONFLICT 7");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "C";
		TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> allDatabaseVersionHeaders = new TreeMap<String, TreeMap<Long, DatabaseVersionHeader>>();
		
		// B
		allDatabaseVersionHeaders.put("B", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {
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
		allDatabaseVersionHeaders.put("C", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {
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
		testFromMachinePerspective(localMachineName, allDatabaseVersionHeaders, expectedTestResult);
	}				
	
	@Test
	public void testWinnersWinner() throws Exception {	 // TODO Extract this from this class, not related to the scenario
		System.out.println("Winners winner test");
		System.out.println("----------------");
		
		/// Input data ///
		String localMachineName = "B";
		TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> allDatabaseVersionHeaders = new TreeMap<String, TreeMap<Long, DatabaseVersionHeader>>();
		
		// A
		allDatabaseVersionHeaders.put("A", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {
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
		allDatabaseVersionHeaders.put("B", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {
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
		allDatabaseVersionHeaders.put("C", TestDatabaseVersionUtil.createDatabaseVersionHeaderMapWithTimeKey(new String[] {
			"C/(C1)/T=1",
			"C/(C2)/T=2",
			"C/(C3)/T=3",	
			"A/(A1,C4)/T=8", // last common
			
			"C/(A1,C5)/T=9", // first conflicting, loses
		}));		
				
		/// Expected results ///
		TestResult expectedTestResult = new TestResult();
		
		expectedTestResult.lastCommonHeader = TestDatabaseVersionUtil.createFromString("A/(A1,C4)/T=8");
		expectedTestResult.firstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A2,C4)/T=9",
			"B", "A/(A2,C4)/T=9",
			"C", "C/(A1,C5)/T=8"			
		});		
		expectedTestResult.winningFirstConflictingDatabaseVersionHeaders = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A2,C4)/T=9",
			"B", "A/(A2,C4)/T=9",
		});		
		expectedTestResult.winnersWinnersLastDatabaseVersionHeader = TestDatabaseVersionUtil.createMapWithMachineKey(new String[] {
			"A", "A/(A5,C4)/T=15"
		}).firstEntry();
				
		/// Perform test ///
		testFromMachinePerspective(localMachineName, allDatabaseVersionHeaders, expectedTestResult);
	}			

	private void testFromMachinePerspective(String localMachineName, TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> allDatabaseVersionHeaders, TestResult expectedTestResult) throws Exception {
		// Print them all
		System.out.println("testFromMachinePerspective('"+localMachineName+"') with database version headers:");
		
		for (String machineName : allDatabaseVersionHeaders.keySet()) {
			System.out.println(machineName+":");
			printMap(allDatabaseVersionHeaders.get(machineName));
		}
			
		// Get 'local' branch
		TreeMap<Long, DatabaseVersionHeader> localDatabaseVersionHeaders = allDatabaseVersionHeaders.get(localMachineName);
		
		// Get all the other ones (clone 'all', and remove local)
		TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> remoteDatabaseVersionHeaders = (TreeMap<String, TreeMap<Long, DatabaseVersionHeader>>) allDatabaseVersionHeaders.clone();
		remoteDatabaseVersionHeaders.remove(localMachineName);
	
		System.out.println("----------");
		
		DatabaseVersionUpdateDetector databaseVersionUpdateDetector = new DatabaseVersionUpdateDetector();
		TestResult actualTestResult = new TestResult();
		
		actualTestResult.lastCommonHeader = databaseVersionUpdateDetector.findLastCommonDatabaseVersionHeader(localDatabaseVersionHeaders, remoteDatabaseVersionHeaders);		
		actualTestResult.firstConflictingDatabaseVersionHeaders = databaseVersionUpdateDetector.findFirstConflictingDatabaseVersionHeader(actualTestResult.lastCommonHeader, localMachineName, localDatabaseVersionHeaders, remoteDatabaseVersionHeaders);
		actualTestResult.winningFirstConflictingDatabaseVersionHeaders = databaseVersionUpdateDetector.findWinningFirstConflictingDatabaseVersionHeaders(actualTestResult.firstConflictingDatabaseVersionHeaders);
		actualTestResult.winnersWinnersLastDatabaseVersionHeader = databaseVersionUpdateDetector.findWinnersWinnersLastDatabaseVersionHeader(actualTestResult.winningFirstConflictingDatabaseVersionHeaders, allDatabaseVersionHeaders);
		
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
