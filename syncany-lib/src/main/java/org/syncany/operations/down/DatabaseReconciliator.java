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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.VectorClock;
import org.syncany.database.VectorClock.VectorClockComparison;
import org.syncany.operations.down.DatabaseBranch.BranchIterator;

/**
 * The database reconciliator implements various parts of the sync down algorithm (see also:
 * {@link DownOperation}). Its main responsibility is to compare the local database to the
 * other clients' delta databases. The final goal of the algorithms described in this class is
 * to determine a winning {@link MemoryDatabase} (or better: a winning database {@link DatabaseBranch}) of
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
 * <p><b>Algorithm (short explanation):</b>
 * <ol>
 *  <li>Go back to the first conflict of all versions</li>
 *  <li>Determine winner of this conflict; follow the winner(s) branch</li>
 *  <li>If another conflict occurs, go to step 2</li>
 * </ol>
 * 
 * <p><b>Algorithm (medium long explanation):</b>
 * <ol>
 *  <li>Determine last versions per client A B C</li>
 *  <li>Determine if there are conflicts between last versions of client, if yes continue</li> 
 *  <li>Determine last common versions between clients</li>
 *  <li>Determine first conflicting versions between clients (= last common version + 1)</li>
 *  <li>Compare first conflicting versions and determine winner</li>
 *  <li>If one client has the winning first conflicting version, take this client's history as a winner</li>
 *  <li>If more than 2 clients are based on the winning first conflicting version, compare their other versions
 *      <ol>
 *        <li>Iterate forward (from conflicting to newer!), and check for conflicts</li>
 *        <li>If a conflict is found, determine the winner and continue the branch of the winner</li>
 *        <li>This must be done until the last (newest!) version of the winning branch is reached</li>
 *      </ol>
 *  </li>
 *  <li>If the local machine loses (= winning first conflicting database version is NOT from the local machine)
 *      <i>and</i> there is a first conflicting database version from the local machine (and possibly more database versions),</li>
 *      <ol>
 *        <li>these database versions must be pruned/deleted from the local database</li>
 *        <li>and these database versions must be merged somehow in the last winning database version</li>
 *      </ol>
 *  </li>
 * </ol>
 * 
 * <p><b>Algorithm (long explanation):</b> 
 * <ul>
 *  <li>{@link #stitchBranches(DatabaseBranches, String, DatabaseBranch) stitchBranches()}: Due to the fact that
 *      Syncany exchanges only delta databases, but the algorithms require a full branch for the
 *      winner-determination, the full per-client branches must be created/derived from all the
 *      downloaded branches.</li>
 *  <li>{@link #findLastCommonDatabaseVersionHeader(DatabaseBranch, DatabaseBranches) findLastCommonDatabaseVersionHeader()}:
 *      Once the full database branches have been stitched together, the algorithm must determine whether there
 *      are any conflicts between the clients' branches. As a first step, a last common 
 *      {@link DatabaseVersionHeader} is determined, i.e. a header that all clients share.</li>
 *  <li>{@link #findFirstConflictingDatabaseVersionHeader(DatabaseVersionHeader, DatabaseBranches) findFirstConflictingDatabaseVersionHeader()}:
 *      By definition, the first conflicting database version between the clients is the database version
 *      following the last common version (= last common database version + 1).</li>
 *  <li>{@link #findWinningFirstConflictingDatabaseVersionHeaders(TreeMap) findWinningFirstConflictingDatabaseVersionHeaders()}:
 *      Comparing the vector clocks of the first conflicting database version headers, the winners can be
 *      determined. This is done using the local timestamps of the database version headers (earliest wins).</li>
 *  <li>{@link #findWinnersWinnersLastDatabaseVersionHeader(TreeMap, DatabaseBranches) findWinnersWinnersLastDatabaseVersionHeader()}:
 *      Having found one or many winning branches (candidates), the last step is to walk forward and compare
 *      the winning branches with each other -- comparing their database version headers. If a set does not match,
 *      a winner is determined until only one client branch remains. This client branch is the final winning branch.</li>
 * </ul>
 * 
 * @see DownOperation
 * @see VectorClock
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Steffen Dangmann <steffen.dangmann@googlemail.com>
 */
// TODO [medium] This class needs some rework, explanations and a code review. It works for now, but its hardly understandable!
public class DatabaseReconciliator {
	//private static final Logger logger = Logger.getLogger(DatabaseReconciliator.class.getSimpleName());
	
	public DatabaseVersionHeader findLastCommonDatabaseVersionHeader(DatabaseBranch localBranch, DatabaseBranches remoteBranches) {
		DatabaseVersionHeader lastCommonDatabaseVersionHeader = null;
		
		for (BranchIterator localBranchIterator = localBranch.iteratorLast(); localBranchIterator.hasPrevious(); ) {
			DatabaseVersionHeader currentLocalDatabaseVersionHeader = localBranchIterator.previous();
			VectorClock currentVectorClock = currentLocalDatabaseVersionHeader.getVectorClock();

			if (isKeyInAllRemoteDatabasesGreaterOrEqual(currentVectorClock, remoteBranches)) {
				lastCommonDatabaseVersionHeader = currentLocalDatabaseVersionHeader;
				break;
			}
		}

		return lastCommonDatabaseVersionHeader;
	}	
	
	private boolean isKeyInAllRemoteDatabasesGreaterOrEqual(VectorClock currentVectorClock, DatabaseBranches remoteDatabaseVersionHeaders) {
		Set<String> remoteClients = remoteDatabaseVersionHeaders.getClients();
		Map<String, Boolean> foundInClientMatrix = initializeFoundInClientMatrix(remoteClients);

		for (String currentRemoteClient : remoteClients) {
			DatabaseBranch remoteBranch = remoteDatabaseVersionHeaders.getBranch(currentRemoteClient);

			for (DatabaseVersionHeader remoteDatabaseVersionHeader : remoteBranch.getAll()) {
				VectorClock remoteVectorClock = remoteDatabaseVersionHeader.getVectorClock();
				VectorClockComparison result = VectorClock.compare(remoteVectorClock, currentVectorClock);
				if (result == VectorClockComparison.GREATER || result == VectorClockComparison.EQUAL) {
					foundInClientMatrix.put(currentRemoteClient, true);
					break;
				}
			}

			if (foundInClientMatrix.get(currentRemoteClient) == false) { 
				return false;
			}
		}

		return isFoundInClientMatrixFullyTrue(foundInClientMatrix);
	}	

	private Map<String, Boolean> initializeFoundInClientMatrix(Set<String> clients) {
		Map<String, Boolean> foundInClientMatrix = new HashMap<String, Boolean>();
		for (String client : clients) {
			foundInClientMatrix.put(client, false);
		}
		return foundInClientMatrix;
	}

	public boolean isFoundInClientMatrixFullyTrue(Map<String, Boolean> foundInClientMatrix) {
		for (Boolean isFound : foundInClientMatrix.values()) {
			if (isFound == false) {
				return false;
			}
		}
		return true;
	}

	public TreeMap<String, DatabaseVersionHeader> findFirstConflictingDatabaseVersionHeader(DatabaseVersionHeader lastCommonHeader, DatabaseBranches allDatabaseVersionHeaders) {
		TreeMap<String, DatabaseVersionHeader> firstConflictingDatabaseVersionHeaders = new TreeMap<String, DatabaseVersionHeader>();
		
		nextClient:	for (String remoteMachineName : allDatabaseVersionHeaders.getClients()) {
			DatabaseBranch branch = allDatabaseVersionHeaders.getBranch(remoteMachineName);
			
			for (Iterator<DatabaseVersionHeader> i = branch.iteratorFirst(); i.hasNext(); ) {
				DatabaseVersionHeader thisDatabaseVersionHeader = i.next();
				
				if (thisDatabaseVersionHeader.equals(lastCommonHeader)) {
					if (i.hasNext()) {
						DatabaseVersionHeader firstConflictingInBranch = i.next();
						firstConflictingDatabaseVersionHeaders.put(remoteMachineName, firstConflictingInBranch);
					}
					else {
						// No conflict here!
					}
					
					continue nextClient;
				} 
			}

			// Last common header not found; Add first as conflict			
			if (branch.size() > 0) {
				DatabaseVersionHeader firstConflictingInBranch = branch.get(0);
				firstConflictingDatabaseVersionHeaders.put(remoteMachineName, firstConflictingInBranch);
			}
		}
		
		return firstConflictingDatabaseVersionHeaders;
	}
	
	public TreeMap<String, DatabaseVersionHeader> findWinningFirstConflictingDatabaseVersionHeaders(
			TreeMap<String, DatabaseVersionHeader> firstConflictingDatabaseVersionHeaders) {
		DatabaseVersionHeader winningFirstConflictingDatabaseVersionHeader = null;

		// Compare all first conflicting ones and take the one with the EARLIEST
		// timestamp
		for (DatabaseVersionHeader databaseVersionHeader : firstConflictingDatabaseVersionHeaders.values()) {
			if (winningFirstConflictingDatabaseVersionHeader == null) {
				winningFirstConflictingDatabaseVersionHeader = databaseVersionHeader;
			} else if (databaseVersionHeader.getDate().before(winningFirstConflictingDatabaseVersionHeader.getDate())) {
				winningFirstConflictingDatabaseVersionHeader = databaseVersionHeader;
			}
		}

		// Find all first conflicting entries with the SAME timestamp as the
		// EARLIEST one (= multiple winning entries possible)
		TreeMap<String, DatabaseVersionHeader> winningFirstConflictingDatabaseVersionHeaders = new TreeMap<String, DatabaseVersionHeader>();

		for (Map.Entry<String, DatabaseVersionHeader> entry : firstConflictingDatabaseVersionHeaders.entrySet()) {
			if (winningFirstConflictingDatabaseVersionHeader.equals(entry.getValue())) {
				winningFirstConflictingDatabaseVersionHeaders.put(entry.getKey(), entry.getValue());
			}
		}

		// If any, find entries that are GREATER than the winners (= successors)
		// TODO [low] Implementation not understandable and ugly
		List<String> removeWinners = new ArrayList<String>();
		TreeMap<String, DatabaseVersionHeader> addWinners = new TreeMap<String, DatabaseVersionHeader>();

		for (Map.Entry<String, DatabaseVersionHeader> winningEntry : winningFirstConflictingDatabaseVersionHeaders.entrySet()) {
			for (Map.Entry<String, DatabaseVersionHeader> aFirstConflictingEntry : firstConflictingDatabaseVersionHeaders.entrySet()) {
				DatabaseVersionHeader winningDatabaseVersionHeader = winningEntry.getValue();
				DatabaseVersionHeader aFirstConflictingDatabaseVersionHeader = aFirstConflictingEntry.getValue();

				if (!winningDatabaseVersionHeader.equals(aFirstConflictingDatabaseVersionHeader)) {
					VectorClockComparison aFirstConflictingDatabaseVersionHeaderIs = VectorClock.compare(
							aFirstConflictingDatabaseVersionHeader.getVectorClock(), winningDatabaseVersionHeader.getVectorClock());

					// We found a greater one. Remove the original winner, and
					// add this entry!
					if (aFirstConflictingDatabaseVersionHeaderIs == VectorClockComparison.GREATER) {
						addWinners.put(aFirstConflictingEntry.getKey(), aFirstConflictingEntry.getValue());
						removeWinners.add(winningEntry.getKey());
					}
				}
			}
		}

		winningFirstConflictingDatabaseVersionHeaders.putAll(addWinners);

		for (String removeWinnerKey : removeWinners) {
			winningFirstConflictingDatabaseVersionHeaders.remove(removeWinnerKey);
		}

		return winningFirstConflictingDatabaseVersionHeaders;
	}

	public Map.Entry<String, DatabaseVersionHeader> findWinnersWinnersLastDatabaseVersionHeader(
			TreeMap<String, DatabaseVersionHeader> winningFirstConflictingDatabaseVersionHeaders,
			DatabaseBranches allDatabaseVersionHeaders) throws Exception {

		if (winningFirstConflictingDatabaseVersionHeaders.size() == 1) {
			String winningMachineName = winningFirstConflictingDatabaseVersionHeaders.firstKey();
			DatabaseVersionHeader winnersWinnersLastDatabaseVersionHeader = allDatabaseVersionHeaders.getBranch(winningMachineName).getLast();

			return new AbstractMap.SimpleEntry<String, DatabaseVersionHeader>(winningMachineName, winnersWinnersLastDatabaseVersionHeader);
		}

		// Algorithm:
		// Iterate over all machines' branches forward, find conflicts and
		// decide who wins

		// 1. Find winners winners positions in branch
		Map<String, Integer> machineBranchPositionIterator = new HashMap<String, Integer>();

		for (String machineName : winningFirstConflictingDatabaseVersionHeaders.keySet()) {
			DatabaseVersionHeader machineWinnersWinner = winningFirstConflictingDatabaseVersionHeaders.get(machineName);
			DatabaseBranch machineBranch = allDatabaseVersionHeaders.getBranch(machineName);

			for (int i=0; i<machineBranch.size(); i++) {
				DatabaseVersionHeader machineDatabaseVersionHeader = machineBranch.get(i);
				
				if (machineWinnersWinner.equals(machineDatabaseVersionHeader)) {
					machineBranchPositionIterator.put(machineName, i);
					break;
				}
			}
		}

		// 2. Compare all, go forward if all are identical
		int machineInRaceCount = winningFirstConflictingDatabaseVersionHeaders.size();

		while (machineInRaceCount > 1) {
			String currentComparisonMachineName = null;
			DatabaseVersionHeader currentComparisonDatabaseVersionHeader = null;

			for (Map.Entry<String, Integer> machineBranchPosition : machineBranchPositionIterator.entrySet()) {
				String machineName = machineBranchPosition.getKey();
				DatabaseBranch machineDatabaseVersionHeaders = allDatabaseVersionHeaders.getBranch(machineName);
				Integer machinePosition = machineBranchPosition.getValue();

				if (machinePosition == null) {
					continue;
				}

				if (machinePosition >= machineDatabaseVersionHeaders.size()) {
					// Eliminate machine in current loop
					// TODO [low] Eliminate machine in current loop, is this correct?
					machineBranchPositionIterator.put(machineName, null);
					machineInRaceCount--;
					
					continue;
				}

				DatabaseVersionHeader currentMachineDatabaseVersionHeader = machineDatabaseVersionHeaders.get(machinePosition);;
				
				if (currentComparisonDatabaseVersionHeader == null) {
					currentComparisonMachineName = machineName;
					currentComparisonDatabaseVersionHeader = currentMachineDatabaseVersionHeader;
				} 
				else {
					VectorClockComparison comparison = VectorClock.compare(currentComparisonDatabaseVersionHeader.getVectorClock(),
							currentMachineDatabaseVersionHeader.getVectorClock());

					if (comparison != VectorClockComparison.EQUAL) {	
						// a. Decide which machine will be eliminated (by timestamp, then name)
						Boolean eliminateComparisonMachine = null; 
						
						if (currentComparisonDatabaseVersionHeader.getDate().before(currentMachineDatabaseVersionHeader.getDate())) {
							// Comparison machine timestamp before current machine timestamp							
							eliminateComparisonMachine = false;
						} 
						else if (currentMachineDatabaseVersionHeader.getDate().before(currentComparisonDatabaseVersionHeader.getDate())) {
							// Current machine timestamp before comparison machine timestamp							
							eliminateComparisonMachine = true;
						} 
						else {
							// Conflicting database version header timestamps are equal
							// Now the alphabet decides: A wins before B!
							
							if (currentComparisonMachineName.compareTo(machineName) < 0) { 																
								eliminateComparisonMachine = false;
							}
							else { 
								eliminateComparisonMachine = true;
							}							
						}
						
						// b. Actually eliminate a machine (comparison machine, or current machine)
						if (eliminateComparisonMachine) {
							machineBranchPositionIterator.put(currentComparisonMachineName, null);
							machineInRaceCount--;

							currentComparisonMachineName = machineName;
							currentComparisonDatabaseVersionHeader = currentMachineDatabaseVersionHeader;							
						}
						else {
							machineBranchPositionIterator.put(machineName, null);
							machineInRaceCount--;
						}
					}
				}
			}

			if (machineInRaceCount > 1) {
				for (String machineName : machineBranchPositionIterator.keySet()) {
					Integer machineCurrentKey = machineBranchPositionIterator.get(machineName);

					if (machineCurrentKey != null) {
						machineBranchPositionIterator.put(machineName, machineCurrentKey+1);
					}
				}
			}
		}

		for (String machineName : machineBranchPositionIterator.keySet()) {
			Integer machineCurrentKey = machineBranchPositionIterator.get(machineName);

			if (machineCurrentKey != null) {
				DatabaseVersionHeader winnersWinnersLastDatabaseVersionHeader = allDatabaseVersionHeaders.getBranch(machineName).getLast();
				return new AbstractMap.SimpleEntry<String, DatabaseVersionHeader>(machineName, winnersWinnersLastDatabaseVersionHeader);
			}
		}

		return null;
	}
	
	public DatabaseBranches stitchBranches(DatabaseBranches unstitchedUnknownBranches, String localClientName, DatabaseBranch localBranch) {
		DatabaseBranches allBranches = unstitchedUnknownBranches.clone();
		
		mergeLocalBranchInRemoteBranches(localClientName, allBranches, localBranch);
		
		Set<DatabaseVersionHeader> allHeaders = gatherAllDatabaseVersionHeaders(allBranches);
		
		completeBranchesWithDatabaseVersionHeaders(allBranches, allHeaders);
		
		return allBranches;
	}

	private void mergeLocalBranchInRemoteBranches(String localClientName, DatabaseBranches allBranches, DatabaseBranch localBranch) {
		if(allBranches.getClients().contains(localClientName)) {
			DatabaseBranch unknownLocalClientBranch = allBranches.getBranch(localClientName);
			
			for (DatabaseVersionHeader header : localBranch.getAll()) {
				if (unknownLocalClientBranch.get(header.getVectorClock()) == null) {
					unknownLocalClientBranch.add(header);
				}
			}
			
			DatabaseBranch sortedClientBranch = sortBranch(unknownLocalClientBranch);
			allBranches.put(localClientName, sortedClientBranch);
		} else if (localBranch.size() > 0)	{
			allBranches.put(localClientName, localBranch);
		}
	}
	
	private Set<DatabaseVersionHeader> gatherAllDatabaseVersionHeaders(DatabaseBranches allBranches) {
		Set<DatabaseVersionHeader> allHeaders = new HashSet<DatabaseVersionHeader>();
		
		for (String client : allBranches.getClients()) {
			DatabaseBranch clientBranch = allBranches.getBranch(client);
			
			for (DatabaseVersionHeader databaseVersionHeader : clientBranch.getAll()) {
				allHeaders.add(databaseVersionHeader);
			}
		}
		
		return allHeaders;
	}

	private void completeBranchesWithDatabaseVersionHeaders(DatabaseBranches allBranches, Set<DatabaseVersionHeader> allHeaders) {
		for (String client : allBranches.getClients()) {
			DatabaseBranch clientBranch = allBranches.getBranch(client);
			if(clientBranch.size() > 0) {
				VectorClock lastVectorClock = clientBranch.getLast().getVectorClock();
				
				for (DatabaseVersionHeader databaseVersionHeader : allHeaders) {
					VectorClock currentVectorClock = databaseVersionHeader.getVectorClock();
					boolean isCurrentVectorClockSmaller = VectorClock.compare(currentVectorClock, lastVectorClock) == VectorClockComparison.SMALLER;
					boolean currentVectorClockExistsInBranch = clientBranch.get(currentVectorClock) != null;
					boolean isInConflict = VectorClock.compare(lastVectorClock,currentVectorClock) == VectorClockComparison.SIMULTANEOUS;
					
					if(!currentVectorClockExistsInBranch && isCurrentVectorClockSmaller && !isInConflict) {
						clientBranch.add(databaseVersionHeader);
					}
				}
				
				DatabaseBranch sortedBranch = sortBranch(clientBranch);
		        allBranches.put(client, sortedBranch);
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
	
    private class DatabaseVersionHeaderComparator implements Comparator<DatabaseVersionHeader> {

        @Override
        public int compare(DatabaseVersionHeader o1, DatabaseVersionHeader o2) {
            VectorClockComparison vectorClockComparison = VectorClock.compare(o1.getVectorClock(), o2.getVectorClock());
           
            if (vectorClockComparison == VectorClockComparison.SIMULTANEOUS) {
                throw new RuntimeException("There must not be a conflict within a branch. VC1: " + o1.getVectorClock() + " - VC2: " + o2.getVectorClock());
            }
           
            if (vectorClockComparison == VectorClockComparison.EQUAL) {
                return 0;
            }
            else if(vectorClockComparison == VectorClockComparison.SMALLER){
            	return -1;
            } 
            else {
            	return 1;
            }
        }
    }
	
	public DatabaseBranch findLosersPruneBranch(DatabaseBranch losersBranch, DatabaseBranch winnersBranch) {
		DatabaseBranch losersPruneBranch = new DatabaseBranch();
		
		boolean pruneBranchStarted = false;
		
		for (int i=0; i<losersBranch.size(); i++) {
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
		
		for (int i=0; i<winnersBranch.size(); i++) {
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

}
