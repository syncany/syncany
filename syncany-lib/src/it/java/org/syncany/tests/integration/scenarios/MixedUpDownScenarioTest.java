/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.integration.scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import org.junit.Ignore;
import org.junit.Test;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

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
 *               (  ,B1,C3)/T=07                         
 *               (A3,B2,C4)/T=16 (first conflict B)      
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
 *  - Last versions conflicts:                (A3,B5,C4)/T=20 || (A6,  ,C4)/T=19 
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
public class MixedUpDownScenarioTest {
	@Test
	@Ignore
	public void testMixedScenario1() throws Exception {
		// Setup 
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		
		// Run 
		clientC.createNewFile("C1");
		clientC.up();
		clientC.createNewFile("C2");
		clientC.up();
		clientC.createNewFile("C3");
		clientC.up();
		
		clientB.down(); // NO CONFLICT
		assertFileListEquals("Client B and C should be on the same versions.", clientB.getLocalFilesExcludeLockedAndNoRead(), clientC.getLocalFilesExcludeLockedAndNoRead());		

		clientC.createNewFile("C4"); // up without down! Evil!
		clientC.up();
		
		clientA.down(); // NO CONFLICT
		assertFileListEquals("Client A and C should be on the same versions.", clientA.getLocalFilesExcludeLockedAndNoRead(), clientC.getLocalFilesExcludeLockedAndNoRead());
		assertEquals("Client A should have C4, client B should not", clientA.getLocalFilesExcludeLockedAndNoRead().size()-1, clientB.getLocalFilesExcludeLockedAndNoRead().size());
		
		clientB.createNewFile("B1,C3");
		clientB.up();
		
		clientA.createNewFile("A1,C4");
		clientA.up();
		clientA.createNewFile("A2,C4");
		clientA.up();
		clientA.createNewFile("A3,C4");
		clientA.up();
		
		clientB.down(); // CONFLICT 1
		fail("Add some asserts");
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		
		clientA.down(); // CONFLICT 2
		clientC.down(); // CONFLICT 3
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertFileListEquals(clientB.getLocalFilesExcludeLockedAndNoRead(), clientC.getLocalFilesExcludeLockedAndNoRead());
		
		clientA.createNewFile("A4,C4");
		clientA.up();
		clientA.createNewFile("A5,C4");
		clientA.up();

		clientC.createNewFile("A3,C5");
		clientC.up();
		clientC.createNewFile("A3,C6");
		clientC.up();
		clientC.createNewFile("A3,C7");
		clientC.up();
		
		clientB.createNewFile("A3,B2,C4");
		clientB.up();				
		clientB.createNewFile("A3,B3,C4");
		clientB.up();		
		clientB.createNewFile("A3,B4,C4");
		clientB.up();	
		
		clientA.createNewFile("A6,C4");
		clientA.up();
		
		clientB.createNewFile("A3,B5,C4");
		clientB.up();	
		
		clientC.createNewFile("A3,C8");
		clientC.up();
				
		clientB.down(); // CONFLICT 4
		clientA.down(); // CONFLICT 5
		clientC.down(); // CONFLICT 6
		
		clientA.up();
		clientA.down();
		
		clientB.up();
		clientB.down();
		
		clientC.up();
		clientC.down();
				
		clientC.up();
		clientB.up();
		
		clientC.down(); // CONFLICT 7
		clientB.down(); // CONFLICT 8
		clientA.down(); // CONFLICT 9
		
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertFileListEquals(clientB.getLocalFilesExcludeLockedAndNoRead(), clientC.getLocalFilesExcludeLockedAndNoRead());
		
		assertEquals("File list count does not match.", 19, clientA.getLocalFilesExcludeLockedAndNoRead().size());
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		clientC.deleteTestData();
	}
}
