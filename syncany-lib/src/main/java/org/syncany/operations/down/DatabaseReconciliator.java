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
package org.syncany.operations.down;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.VectorClock;
import org.syncany.database.VectorClock.VectorClockComparison;

/**
 * The database reconciliator implements various parts of the sync down algorithm (see also:
 * {@link DownOperation}). Its main responsibility is to compare the local database to the
 * other clients' delta databases. The final goal of the algorithms described in this class is
 * to determine a winning {@link MemoryDatabase} (or better: a winning {@link DatabaseBranch}) of
 * a client.
 * 
 * <p>All algorithm parts largely rely on the comparison of a client's database branch, i.e. its
 * committed set of {@link DatabaseVersion}s. Instead of comparing the entire database versions
 * of the different clients, however, the comparisons solely rely on the  {@link DatabaseVersionHeader}s.
 * In particular, most of them only compare the {@link VectorClock}. If the vector clocks are 
 * in conflict (= simultaneous), the local timestamp is used as a final decision (oldest wins).
 * 
 * <p>Because there are many ways to say it, there are a few explanations to the algorithms:
 * 
 * <p><b>Algorithm:</b>
 * <ol>
 *  <li>Input: Local branch, unknown remote branches</li>
 *  <li>Stitch branches together, so that all branches also contain other clients' database version headers</li> 
 *  <li>Using the full branches of all clients, walk forward and compare database versions of the same index to one another:
 *      <ol>
 *        <li>Iterate forward (from position 0 to newer!), and check for conflicts</li>
 *        <li>If a conflict is found, determine the winner and eliminate the losing machine, and continue with the remaining machine branches</li>
 *        <li>This must be done until the last (newest!) version of the winning branch is reached</li>
 *      </ol>
 *  </li>
 *  <li>If the local machine loses (= winning branch is NOT from the local machine)
 *      <i>and</i> there is at least one conflicting database version from the local machine (and possibly more database versions),</li>
 *      <ol>
 *        <li>these database versions must be pruned/deleted from the local database</li>
 *        <li>and these database versions must be merged somehow in the last winning database version</li>
 *      </ol>
 *  </li>
 * </ol>
 * 
 * @see DownOperation
 * @see VectorClock
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Steffen Dangmann <steffen.dangmann@googlemail.com>
 */
public class DatabaseReconciliator {
	private static final Logger logger = Logger.getLogger(DatabaseReconciliator.class.getSimpleName());

	/**
	 * Returns {@link DatabaseBranches} object representing the full branches per client/machine, i.e. the 
	 * branches containing other clients' database version headers.
	 * 
	 * <p>The algorithm takes the partial client branches and stitches them together so they can be easily 
	 * compared by walking through the branches. 
	 * 
	 * <p>Due to the fact that Syncany exchanges only delta databases, but the winners algorithm in {@link #findWinnerBranch(DatabaseBranches)}
	 * requires a full branch for the winner-determination, the full per-client branches must be created/derived from all the
	 * downloaded branches. This method creates these full branches.
	 * 
	 * @param unstitchedUnknownBranches Unstitched branches per client (one client's database version headers per branch) 
	 * @param localClientName Local client name
	 * @param unstitchedLocalBranch Unstitched local branch (only local database version headers)
	 * @return Returns all stitched branches (full branches, containing database version headers of all clients)
	 */
	public DatabaseBranches stitchBranches(DatabaseBranches unstitchedUnknownBranches, String localClientName, DatabaseBranch unstitchedLocalBranch) {		
		DatabaseBranches allUnstitchedBranches = mergeLocalBranchInRemoteBranches(localClientName, unstitchedLocalBranch, unstitchedUnknownBranches);
		DatabaseBranches allStitchedBranches = stitchAllBranches(allUnstitchedBranches);
		
		return allStitchedBranches;
	}

	/**
	 * Implements the core synchronization algorithm as described {@link DatabaseReconciliator in the class description}.
	 * 
	 * @param localMachineName Client name of the local machine (required for branch stitching)
	 * @param localBranch Local branch, created from the local database
	 * @param unknownRemoteBranches Newly downloaded unknown remote branches (incomplete branches; will be stitched)
	 * @return Returns the branch of the winning client
	 */
	public Map.Entry<String, DatabaseBranch> findWinnerBranch(DatabaseBranches allStitchedBranches)
			throws Exception {
		
		Entry<String, DatabaseBranch> winnersNameAndBranch = findWinnersNameAndBranch(allStitchedBranches);

		if (winnersNameAndBranch != null) {
			String winnersName = winnersNameAndBranch.getKey();
			DatabaseBranch winnersBranch = allStitchedBranches.getBranch(winnersName);
	
			if (logger.isLoggable(Level.INFO)) {
				logger.log(Level.INFO, "- Winner is " + winnersName + " with branch: ");
	
				for (DatabaseVersionHeader databaseVersionHeader : winnersBranch.getAll()) {
					logger.log(Level.INFO, "  + " + databaseVersionHeader);
				}			
			}
	
			return new AbstractMap.SimpleEntry<String, DatabaseBranch>(winnersName, winnersBranch);
		}
		else {
			return null;
		}
	}
	
	public DatabaseBranch findLosersPruneBranch(DatabaseBranch losersBranch, DatabaseBranch winnersBranch) {
		DatabaseBranch losersPruneBranch = new DatabaseBranch();

		boolean pruneBranchStarted = false;

		for (int i = 0; i < losersBranch.size(); i++) {
			if (pruneBranchStarted) {
				losersPruneBranch.add(losersBranch.get(i));
			}
			else if (i < winnersBranch.size() && !losersBranch.get(i).equals(winnersBranch.get(i))) {
				pruneBranchStarted = true;
				losersPruneBranch.add(losersBranch.get(i));
			}
		}

		return losersPruneBranch;
	}

	public DatabaseBranch findWinnersApplyBranch(DatabaseBranch losersBranch, DatabaseBranch winnersBranch) {
		DatabaseBranch winnersApplyBranch = new DatabaseBranch();

		boolean applyBranchStarted = false;

		for (int i = 0; i < winnersBranch.size(); i++) {
			if (!applyBranchStarted) {
				if (i >= losersBranch.size() || !losersBranch.get(i).equals(winnersBranch.get(i))) {
					applyBranchStarted = true;
				}
			}

			if (applyBranchStarted) {
				winnersApplyBranch.add(winnersBranch.get(i));
			}
		}

		return winnersApplyBranch;
	}

	/**
	 * Algorithm to find the winner's database branch (client name and branch).
	 * The winner's branch is used to determine the local file system actions.
	 * 
	 * <p>Basic algorithm: Iterate over all machines' branches forward, find conflicts and
	 * decide who wins. The following numbers correspond to the comments in the code
	 * 
	 * <ol>
	 *  <li>The algorithm first checks whether a winner comparison is even necessary. If there is only one
	 *      machine, it simply returns this machine as the winner.</li>
	 *  <li>If there is more than one machine, the 'race' starts at position 0 of each of the clients' branch.
	 *      The algorithm starts a 'race' in which the database version headers of two machines are compared. If the
	 *      two database version headers are equal, both machines are left in the race. If they are not equal,
	 *      only the 'winner' stays in the race. This is repeated for each position of the machines' branches.
	 *      See the example below for a more graphic representation.</li>
	 *  <li>Once only one winner remains, the winner's name and branch is returned.</li>
	 * </ol> 
	 * 
	 * <p><b>Illustration:</b><br />
	 * Suppose the following branches exist. 
	 * Naming: <em>created-by / vector clock / local time</em>.
	 * 
	 * <pre>
	 *    A               B                C
	 * --|-------------------------------------------------
	 * 0 | A/(A1)/T=10    A/(A1)/T=10      A/(A1)/T=10      
	 * 1 | A/(A2)/T=13    A/(A2)/T=13      C/(A1,C1)/T=14
	 * 2 | A/(A3)/T=19    A/(A3)/T=19      C/(A1,C2)/T=15
	 * 3 | A/(A4)/T=23    B/(A3,B1)/T=20   
	 * </pre>
	 * 
	 * The algorithm input will be the database version headers in line 1 (= first database
	 * version headers of each client's branch). 
	 * 
	 * <p>The algorithm will compare the database version headers moving forward, starting from position 0:
	 * <pre>
	 * Positions       1st machine       2nd machine
	 * -----------------------------------------------------------------------------
	 * Round 1:
	 * A[0] vs. B[0]   A: A/(A1)/T=10    B: A/(A1)/T=10      // Equal, no eliminations
	 * A[0] vs. C[0]   A: A/(A1)/T=10    C: A/(A1)/T=10      // Equal, no eliminations
	 * 
	 * Round 2:
	 * A[1] vs. B[1]   A: A/(A2)/T=13    B: A/(A2)/T=13      // Equal, no eliminations
	 * A[1] vs. C[1]   A: A/(A2)/T=13    C: C/(A1,C1)/T=14   // 13<14, A wins, eliminate C
	 * 
	 * Round 3 (C eliminated):
	 * A[2] vs. B[1]   A: A/(A3)/T=19    B: A/(A3)/T=19      // Equal, no eliminations
	 *      
	 * Round 4:
	 * A[3] vs. B[3]   A: A/(A4)/T=23    B: B/(A3,B1)/T=20   // 20<23, B wins, eliminate A
	 * 
	 * // B wins!
	 * </pre>
	 * 
	 * @param allStitchedBranches All stitched branches of all machines (including local)
	 * @return Returns the name and the branch of the winning machine 
	 */
	private Entry<String, DatabaseBranch> findWinnersNameAndBranch(DatabaseBranches allStitchedBranches) {
		// 1. If there is only one conflicting database version header, return it (no need for a complex algorithm)
		if (allStitchedBranches.getClients().size() == 1) {
			String winningMachineName = allStitchedBranches.getClients().iterator().next();
			DatabaseBranch winnersBranch = allStitchedBranches.getBranch(winningMachineName);

			return new AbstractMap.SimpleEntry<String, DatabaseBranch>(winningMachineName, winnersBranch);
		}

		// 2. Find position of first conflicting header in branch (per client)
		Map<String, Integer> clientPositionCounter = initClientPositionCounter(allStitchedBranches);

		// 3. Compare all, go forward if all are identical
		int machineInRaceCount = clientPositionCounter.size();		
		
		while (machineInRaceCount > 1) {
			String firstMachineName = null;
			DatabaseVersionHeader firstMachineDatabaseVersionHeader = null;

			for (Map.Entry<String, Integer> secondMachineNamePositionEntry : clientPositionCounter.entrySet()) {
				// 3a. Get second machine and make sure we can use it (= it hasn't been eliminated before)
				
				// - Get machine name, position of next database version to be compared, and the machine branch
				String secondMachineName = secondMachineNamePositionEntry.getKey();
				Integer secondMachinePosition = secondMachineNamePositionEntry.getValue();				

				// - If machine position is 'null', it has been marked 'eliminated'
				if (secondMachinePosition == null) {
					continue;
				}

				// - If machine position is greater than the machine's branch size (out of bound), 
				//   eliminate the machine (= position to 'null')
				DatabaseBranch secondMachineBranch = allStitchedBranches.getBranch(secondMachineName);

				if (secondMachinePosition >= secondMachineBranch.size()) {
					clientPositionCounter.put(secondMachineName, null);
					machineInRaceCount--;

					continue;
				}

				DatabaseVersionHeader secondMachineDatabaseVersionHeader = secondMachineBranch.get(secondMachinePosition);
				
				// 3b. Now compare 'firstMachine*' and 'secondMachine*'
				
				// If this is the first iteration of the loop, there is nothing to compare it to.
				if (firstMachineDatabaseVersionHeader == null) {
					firstMachineName = secondMachineName;
					firstMachineDatabaseVersionHeader = secondMachineDatabaseVersionHeader;
				}				
				else {
					// Compare the two machines 'firstMachine*' and 'secondMachine*'
					// Keep the winner, eliminate the loser
					
					VectorClockComparison comparison = VectorClock.compare(firstMachineDatabaseVersionHeader.getVectorClock(),
							secondMachineDatabaseVersionHeader.getVectorClock());

					if (comparison != VectorClockComparison.EQUAL) {
						Boolean eliminateFirstMachine = determineEliminateMachine(firstMachineName, firstMachineDatabaseVersionHeader,
								secondMachineName, secondMachineDatabaseVersionHeader);

						if (eliminateFirstMachine) {
							clientPositionCounter.put(firstMachineName, null);
							machineInRaceCount--;

							firstMachineName = secondMachineName;
							firstMachineDatabaseVersionHeader = secondMachineDatabaseVersionHeader;
						}
						else {
							clientPositionCounter.put(secondMachineName, null);
							machineInRaceCount--;
						}
					}
				}
			}

			// 3c. If more than one machine are still in the race, increase positions
			if (machineInRaceCount > 1) {
				increaseBranchPosition(clientPositionCounter);				
			}
		}

		// 4. Return the last remaining machine and its last database version header (= winner!)
		for (String machineName : clientPositionCounter.keySet()) {
			Integer machineCurrentPosition = clientPositionCounter.get(machineName);

			if (machineCurrentPosition != null) {
				DatabaseBranch winnersBranch = allStitchedBranches.getBranch(machineName);
				return new AbstractMap.SimpleEntry<String, DatabaseBranch>(machineName, winnersBranch);
			}
		}

		return null;
	}	

	/**
	 * This method increases per-machine positions. 
	 * It ignores 'eliminated' machines (= machines with position 'null'). 
	 * 
	 * <p>This algorithm is part of the 
	 * {@link #findWinnersNameAndBranch(DatabaseBranches)} algorithm.
	 */
	private void increaseBranchPosition(Map<String, Integer> machineInBranchPosition) {
		for (String machineName : machineInBranchPosition.keySet()) {
			Integer machineCurrentPosition = machineInBranchPosition.get(machineName);

			if (machineCurrentPosition != null) {
				Integer machineNextPosition = machineCurrentPosition + 1;
				machineInBranchPosition.put(machineName, machineNextPosition);
			}
		}
	}

	/**
	 * Determines which of the two machines will be eliminated, given the two database version headers.
	 * 
	 * <p>This method first compares the timestamps of the two given database version headers, and if they
	 * are equal, uses the machines' names to determine the machine to be eliminated.
	 * 
	 * <p>This algorithm is part of the 
	 * {@link #findWinnersNameAndBranch(DatabaseBranches)} algorithm.
	 * 
	 * @return Returns true if the first machine must be eliminated, false if the second machine must be eliminated 
	 */
	private Boolean determineEliminateMachine(String firstComparisonMachineName, DatabaseVersionHeader firstComparisonDatabaseVersionHeader,
			String secondMachineName, DatabaseVersionHeader secondMachineDatabaseVersionHeader) {

		// a. Decide which machine will be eliminated (by timestamp, then name)
		Boolean eliminateComparisonMachine = null;

		if (firstComparisonDatabaseVersionHeader.getDate().before(secondMachineDatabaseVersionHeader.getDate())) {
			// Comparison machine timestamp before current machine timestamp
			eliminateComparisonMachine = false;
		}
		else if (secondMachineDatabaseVersionHeader.getDate().before(firstComparisonDatabaseVersionHeader.getDate())) {
			// Current machine timestamp before comparison machine timestamp
			eliminateComparisonMachine = true;
		}
		else {
			// Conflicting database version header timestamps are equal
			// Now the alphabet decides: A wins before B!

			if (firstComparisonMachineName.compareTo(secondMachineName) < 0) {
				eliminateComparisonMachine = false;
			}
			else {
				eliminateComparisonMachine = true;
			}
		}
		
		return eliminateComparisonMachine;
	}

	private Map<String, Integer> initClientPositionCounter(DatabaseBranches allStitchedBranches) {
		Map<String, Integer> clientPositionCounter = new HashMap<String, Integer>();

		for (String client : allStitchedBranches.getClients()) {
			clientPositionCounter.put(client, 0);
		}

		return clientPositionCounter;
	}

	private DatabaseBranches mergeLocalBranchInRemoteBranches(String localClientName, DatabaseBranch localBranch, DatabaseBranches unstitchedUnknownBranches) {
		DatabaseBranches allUnstitchedBranches = unstitchedUnknownBranches.clone();
		
		if (allUnstitchedBranches.getClients().contains(localClientName)) {
			DatabaseBranch unknownLocalClientBranch = allUnstitchedBranches.getBranch(localClientName);

			for (DatabaseVersionHeader header : localBranch.getAll()) {
				if (unknownLocalClientBranch.get(header.getVectorClock()) == null) {
					unknownLocalClientBranch.add(header);
				}
			}

			DatabaseBranch sortedClientBranch = sortBranch(unknownLocalClientBranch);
			allUnstitchedBranches.put(localClientName, sortedClientBranch);
		}
		else if (localBranch.size() > 0) {
			allUnstitchedBranches.put(localClientName, localBranch);
		}
		
		return allUnstitchedBranches;
	}

	
	private DatabaseBranches stitchAllBranches(DatabaseBranches allUnstitchedBranches) {
		DatabaseBranches allStitchedBranches = new DatabaseBranches();
		
		for (String client : allUnstitchedBranches.getClients()) {
			DatabaseBranch clientBranch = allUnstitchedBranches.getBranch(client);
			
			if (clientBranch.size() > 0) {
				 logger.log(Level.FINE, "Branch for '" + client + "' is NOT empty. Stitching ...");
				
				DatabaseBranch newUnsortedClientBranch = stitchClientBranchFromOtherBranches(client, clientBranch, allUnstitchedBranches);
				DatabaseBranch newSortedClientBranch = sortBranch(newUnsortedClientBranch);
				
				allStitchedBranches.put(client, newSortedClientBranch);
			}
		}
		
		return allStitchedBranches;
	}
	
	private DatabaseBranch stitchClientBranchFromOtherBranches(String client, DatabaseBranch clientBranch, DatabaseBranches allUnstitchedBranches) {		
		DatabaseBranch newClientBranch = new DatabaseBranch();		
		VectorClock otherClientIndices = new VectorClock();				
		
		for (int clientBranchIndex=0; clientBranchIndex<clientBranch.size(); clientBranchIndex++) {
			DatabaseVersionHeader clientDatabaseVersionHeader = clientBranch.get(clientBranchIndex);								
			VectorClock clientVectorClock = clientDatabaseVersionHeader.getVectorClock();					
			
			 logger.log(Level.FINE, " - Comparing local " + clientDatabaseVersionHeader);

			for (String otherClient : allUnstitchedBranches.getClients()) {
				boolean sameClient = otherClient.equals(client);
				
				if (!sameClient) {
					DatabaseBranch otherClientBranch = allUnstitchedBranches.getBranch(otherClient);
					attachToClientBranchFromOtherBranch(newClientBranch, clientVectorClock, otherClient, otherClientBranch, otherClientIndices);					
				}
			}
			
			 logger.log(Level.FINE, "   * Adding own " + clientDatabaseVersionHeader);
			newClientBranch.add(clientDatabaseVersionHeader);
		}

		return newClientBranch;
	}

	private void attachToClientBranchFromOtherBranch(DatabaseBranch clientBranch, VectorClock clientVectorClock, String otherClient, DatabaseBranch otherClientBranch, VectorClock otherClientIndices) {		
		long otherClientBranchIndex = otherClientIndices.get(otherClient);		
		boolean endOfBranch = otherClientBranchIndex >= otherClientBranch.size();
		
		while (!endOfBranch) {
			DatabaseVersionHeader otherClientDatabaseVersionHeader = otherClientBranch.get((int) otherClientBranchIndex);								
			VectorClock otherClientVectorClock = otherClientDatabaseVersionHeader.getVectorClock();
			
			boolean isOtherClientVectorClockSmaller = VectorClock.compare(otherClientVectorClock, clientVectorClock) == VectorClockComparison.SMALLER;
			boolean otherClientVectorClockExistsInBranch = clientBranch.get(otherClientVectorClock) != null;
			boolean isInConflict = VectorClock.compare(clientVectorClock, otherClientVectorClock) == VectorClockComparison.SIMULTANEOUS;
			
			if (!otherClientVectorClockExistsInBranch && isOtherClientVectorClockSmaller && !isInConflict) {
				 logger.log(Level.FINE, "   * [From " + otherClient + ", Index " + otherClientBranchIndex + "] Adding " + otherClientDatabaseVersionHeader);
				
				clientBranch.add(otherClientDatabaseVersionHeader);								
				otherClientBranchIndex = otherClientIndices.incrementClock(otherClient);
				
				endOfBranch = otherClientBranchIndex >= otherClientBranch.size();
			}
			else {
				if (isInConflict) {
					otherClientIndices.setClock(otherClient, Long.MAX_VALUE);	
				}
				
				endOfBranch = true;
			}						
		}
	}

	private DatabaseBranch sortBranch(DatabaseBranch clientBranch) {
		List<DatabaseVersionHeader> branchCopy = new ArrayList<DatabaseVersionHeader>(clientBranch.getAll());
		Collections.sort(branchCopy, new DatabaseVersionHeaderComparator());
		
		DatabaseBranch sortedBranch = new DatabaseBranch();
		sortedBranch.addAll(branchCopy);
		
		return sortedBranch;
	}
}
