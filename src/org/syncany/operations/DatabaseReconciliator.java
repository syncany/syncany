package org.syncany.operations;

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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.database.Branch;
import org.syncany.database.Branch.BranchIterator;
import org.syncany.database.Branches;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.VectorClock;
import org.syncany.database.VectorClock.VectorClockComparison;

/*
 * This class implements various parts of the sync down algorithm. Test scenarios are available
 * in the DatabaseVersionUpdateDetectorTest class.
 * 
 * ALGORITHM B
 * ----------------------------------------------------------------------------------------------------
 * 
 *  Algorithm:
 *   - Determine last versions per client A B C
 *   - Determine if there are conflicts between last versions of client, if yes continue 
 *   - Determine last common versions between clients
 *   - Determine first conflicting versions between clients (= last common version + 1)
 *   - Compare first conflicting versions and determine winner
 *
 *   - If one client has the winning first conflicting version, take this client's history as a winner
 *   - If more than 2 clients are based on the winning first conflicting version, compare their other versions
 *      + Iterate forward (from conflicting to newer!), and check for conflicts 
 *      + If a conflict is found, determine the winner and continue the branch of the winner
 *      + This must be done until the last (newest!) version of the winning branch is reached
 *    - If the local machine loses (= winning first conflicting database version is NOT from the local machine)
 *      AND there is a first conflicting database version from the local machine (and possibly more database versions),
 *       (1) these database versions must be pruned/deleted from the local database
 *       (2) and these database versions must be merged somehow in the last winning database version 
 *	  - ...
 *     
 *  In short:
 *    1. Go back to the first conflict of all versions
 *    2. Determine winner of this conflict. Follow the winner(s) branch.
 *    3. If another conflict occurs, go to step 2.
 *   
 *  Issues:
 *   - When db-b-1 is not applied, it is re-downloaded every time by clients A and C
 *     until B uploads a consolidated version
 */
public class DatabaseReconciliator {
	private static final Logger logger = Logger.getLogger(DatabaseReconciliator.class.getSimpleName());
	
	public DatabaseVersionHeader findLastCommonDatabaseVersionHeader(Branch localBranch, Branches remoteBranches) {
		DatabaseVersionHeader lastCommonDatabaseVersionHeader = null;
		
		for (BranchIterator localBranchIterator = localBranch.iteratorLast(); localBranchIterator.hasPrevious(); ) {
			DatabaseVersionHeader currentLocalDatabaseVersionHeader = localBranchIterator.previous();
			VectorClock currentVectorClock = currentLocalDatabaseVersionHeader.getVectorClock();

			if (isKeyInAllRemoteDatabasesGreaterOrEqual(currentVectorClock, remoteBranches)) {
				lastCommonDatabaseVersionHeader = currentLocalDatabaseVersionHeader;
			}
		}

		return lastCommonDatabaseVersionHeader;
	}	
	
	private boolean isKeyInAllRemoteDatabasesGreaterOrEqual(VectorClock currentVectorClock, Branches remoteDatabaseVersionHeaders) {
		Set<String> remoteClients = remoteDatabaseVersionHeaders.getClients();
		Map<String, Boolean> foundInClientMatrix = initializeFoundInClientMatrix(remoteClients);

		for (String currentRemoteClient : remoteClients) {
			Branch remoteBranch = remoteDatabaseVersionHeaders.getBranch(currentRemoteClient);

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

	private boolean isFoundInClientMatrixFullyTrue(Map<String, Boolean> foundInClientMatrix) {
		for (Boolean isFound : foundInClientMatrix.values()) {
			if (isFound == false) {
				return false;
			}
		}
		return true;
	}

	public TreeMap<String, DatabaseVersionHeader> findFirstConflictingDatabaseVersionHeader(DatabaseVersionHeader lastCommonHeader, Branches allDatabaseVersionHeaders) {
		TreeMap<String, DatabaseVersionHeader> firstConflictingDatabaseVersionHeaders = new TreeMap<String, DatabaseVersionHeader>();
		
		nextClient:	for (String remoteMachineName : allDatabaseVersionHeaders.getClients()) {
			Branch branch = allDatabaseVersionHeaders.getBranch(remoteMachineName);
			
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
		// TODO [high] this method curently does not catch the scenario in which two first winning conflict headers have the same timestamp
		//             this could be baaad, though very unlikely
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
			Branches allDatabaseVersionHeaders) throws Exception {

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
			Branch machineBranch = allDatabaseVersionHeaders.getBranch(machineName);

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
				Branch machineDatabaseVersionHeaders = allDatabaseVersionHeaders.getBranch(machineName);
				Integer machinePosition = machineBranchPosition.getValue();

				if (machinePosition == null) {
					continue;
				}

				if (machinePosition >= machineDatabaseVersionHeaders.size()) {
					// Eliminate machine in current loop
					// TODO [high] Eliminate machine in current loop, is this correct?
					machineBranchPositionIterator.put(machineName, null);
					machineInRaceCount--;
					
					continue;
				}

				DatabaseVersionHeader currentMachineDatabaseVersionHeader = machineDatabaseVersionHeaders.get(machinePosition);;
				
				if (currentComparisonDatabaseVersionHeader == null) {
					currentComparisonMachineName = machineName;
					currentComparisonDatabaseVersionHeader = currentMachineDatabaseVersionHeader;
				} else {
					VectorClockComparison comparison = VectorClock.compare(currentComparisonDatabaseVersionHeader.getVectorClock(),
							currentMachineDatabaseVersionHeader.getVectorClock());

					if (comparison != VectorClockComparison.EQUAL) {
						if (currentComparisonDatabaseVersionHeader.getDate().before(currentMachineDatabaseVersionHeader.getDate())) {
							// Eliminate machine in current loop

							machineBranchPositionIterator.put(machineName, null);
							machineInRaceCount--;
						} else if (currentMachineDatabaseVersionHeader.getDate().before(
								currentComparisonDatabaseVersionHeader.getDate())) {
							// Eliminate comparison machine

							machineBranchPositionIterator.put(currentComparisonMachineName, null);
							machineInRaceCount--;

							currentComparisonMachineName = machineName;
							currentComparisonDatabaseVersionHeader = currentMachineDatabaseVersionHeader;
						} else {
							throw new Exception("This should not happen."); // TODO [medium] If database header timestamps are equal, compare names
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

	@Deprecated
	public Branches stitchBranches2(Branches unstitchedUnknownBranches, String localClientName, Branch localBranch) {
		Branches allStitchedBranches = new Branches();
		
		// First stitch unknown remote branches 
		for (String remoteClientName : unstitchedUnknownBranches.getClients()) {
			logger.log(Level.INFO, "  + Stitching {0}, going backwards ..", remoteClientName);	
			List<DatabaseVersionHeader> reverseStitchedBranch = new ArrayList<DatabaseVersionHeader>();
			Branch branch = unstitchedUnknownBranches.getBranch(remoteClientName);
			logger.log(Level.INFO, "    - Current branch is: "+branch);
			
			// Interweave deltas
			for (int i=branch.size()-1; i>=0; i--) {
				DatabaseVersionHeader databaseVersionHeader = branch.get(i);				

				logger.log(Level.INFO, "    - Adding "+databaseVersionHeader+" (from "+remoteClientName+"'s delta branch)");
				reverseStitchedBranch.add(databaseVersionHeader);
				
				while (databaseVersionHeader.getPreviousClient() != null && !remoteClientName.equals(databaseVersionHeader.getPreviousClient())) {
					Branch previousClientBranch = unstitchedUnknownBranches.getBranch(databaseVersionHeader.getPreviousClient());

					if (previousClientBranch == null) {
						break; // The rest must be in the local branch
					}
					 
					DatabaseVersionHeader previousDatabaseVersionHeader = previousClientBranch.get(databaseVersionHeader.getPreviousVectorClock());

					if (previousDatabaseVersionHeader == null) {
						//throw new RuntimeException("Unable to find previous database version header in branch of '"+databaseVersionHeader.getPreviousClient()+"' using vector clock "+databaseVersionHeader.getPreviousVectorClock());
						break; // The rest must be in the local branch
					}
					
					logger.log(Level.INFO, "    - Adding "+previousDatabaseVersionHeader+" (from "+databaseVersionHeader.getPreviousClient()+")");					
					reverseStitchedBranch.add(previousDatabaseVersionHeader);
					
					databaseVersionHeader = previousDatabaseVersionHeader;
				}
			}			
			
			// Attach local version's part (if necessary)
			boolean firstRemoteHeadersPreviousVectorClockEmpty = reverseStitchedBranch.size() > 0
					&& reverseStitchedBranch.get(reverseStitchedBranch.size()-1).getPreviousVectorClock().size() > 0;
			
			boolean localBranchNotEmpty = localBranch.size() > 0;
			
			if (firstRemoteHeadersPreviousVectorClockEmpty && localBranchNotEmpty) {				
				DatabaseVersionHeader remoteFirstDatabaseVersionHeader = reverseStitchedBranch.get(reverseStitchedBranch.size()-1);
				VectorClock remotePossiblyOverlappingVectorClock = remoteFirstDatabaseVersionHeader.getPreviousVectorClock();
				
				boolean foundLocalOverlap = false;
				
				for (int localBranchPos=localBranch.size()-1; localBranchPos>=0; localBranchPos--) {				
					DatabaseVersionHeader localLastDatabaseVersionHeader = localBranch.get(localBranchPos);
					VectorClock localLastVectorClock = localLastDatabaseVersionHeader.getVectorClock();
					
					// They overlap! Attach!
					if (VectorClock.compare(localLastVectorClock, remotePossiblyOverlappingVectorClock) == VectorClockComparison.EQUAL) {					
						for (int i=localBranchPos; i>=0; i--) {						
							DatabaseVersionHeader localDatabaseVersionHeader = localBranch.get(i);
							
							logger.log(Level.INFO, "    - Adding "+localDatabaseVersionHeader+" (from local branch, i.e. from "+localClientName+")");						
							reverseStitchedBranch.add(localDatabaseVersionHeader);
						} 
						
						foundLocalOverlap = true;
						break;
					}	
				}
				
				if (!foundLocalOverlap) {
					logger.log(Level.INFO, "    - Found NO OVERLAP with local branch for first remote version "+remoteFirstDatabaseVersionHeader);
					logger.log(Level.INFO, "      --> Disconnected branch -- due to previous conflict. Emptying branch contents. SEEMS IRRELEVANT!");
					reverseStitchedBranch.clear();
					
					// TODO [lowest] Is this right? Is this still relevant. Method is deprecated
				}
			}
			
			// Now reverse again
			Branch stitchedBranch = new Branch();
			
			for (int i=reverseStitchedBranch.size()-1; i>=0; i--) {
				stitchedBranch.add(reverseStitchedBranch.get(i));
			}
			
			logger.log(Level.INFO, "    - Stitched branch is: "+stitchedBranch);
			
			allStitchedBranches.add(remoteClientName, stitchedBranch);
		}
		
		// Add full local branch (if it does not exist in allBranches)
		if (allStitchedBranches.getBranch(localClientName) == null) {
			allStitchedBranches.add(localClientName, localBranch);
		}
		
		return allStitchedBranches;
	}	
	
	public Branches stitchBranches(Branches unstitchedUnknownBranches, String localClientName, Branch localBranch) {
		Branches allBranches = unstitchedUnknownBranches.clone();
		
		mergeLocalBranchInRemoteBranches(localClientName, allBranches, localBranch);
		
		Set<DatabaseVersionHeader> allHeaders = gatherAllDatabaseVersionHeaders(allBranches);
		
		completeBranchesWithDatabaseVersionHeaders(allBranches, allHeaders);
		
		return allBranches;
	}

	private void mergeLocalBranchInRemoteBranches(String localClientName, Branches allBranches, Branch localBranch) {
		if(allBranches.getClients().contains(localClientName)) {
			Branch unknownLocalClientBranch = allBranches.getBranch(localClientName);
			
			for (DatabaseVersionHeader dbvh : localBranch.getAll()) {
				if(unknownLocalClientBranch.get(dbvh.getVectorClock()) == null) {
					unknownLocalClientBranch.add(dbvh);
				}
			}
			
			Branch sortedClientBranch = sortBranch(unknownLocalClientBranch);
			allBranches.add(localClientName, sortedClientBranch);
		} else if(localBranch.size() > 0)	{
			allBranches.add(localClientName, localBranch);
		}
	}
	
	private Set<DatabaseVersionHeader> gatherAllDatabaseVersionHeaders(Branches allBranches) {
		Set<DatabaseVersionHeader> allHeaders = new HashSet<DatabaseVersionHeader>();
		
		for (String client : allBranches.getClients()) {
			Branch clientBranch = allBranches.getBranch(client);
			
			for (DatabaseVersionHeader databaseVersionHeader : clientBranch.getAll()) {
				allHeaders.add(databaseVersionHeader);
			}
		}
		
		return allHeaders;
	}

	private void completeBranchesWithDatabaseVersionHeaders(Branches allBranches, Set<DatabaseVersionHeader> allHeaders) {
		for (String client : allBranches.getClients()) {
			Branch clientBranch = allBranches.getBranch(client);
			if(clientBranch.size() > 0) {
				VectorClock lastVectorClock = clientBranch.getLast().getVectorClock();
				
				for (DatabaseVersionHeader databaseVersionHeader : allHeaders) {
					VectorClock currentVectorClock = databaseVersionHeader.getVectorClock();
					boolean isCurrentVectorClockSmaller = VectorClock.compare(currentVectorClock, lastVectorClock) == VectorClockComparison.SMALLER;
					boolean currentVectorClockExistsInBranch = clientBranch.get(currentVectorClock) != null;
					
					if(!currentVectorClockExistsInBranch && isCurrentVectorClockSmaller) {
						clientBranch.add(databaseVersionHeader);
					}
				}
				
				Branch sortedBranch = sortBranch(clientBranch);
		        allBranches.add(client, sortedBranch);
			}
		}
	}

	private Branch sortBranch(Branch clientBranch) {
		List<DatabaseVersionHeader> branchCopy = new ArrayList<DatabaseVersionHeader>(clientBranch.getAll());
		Collections.sort(branchCopy, new DatabaseVersionHeaderComparator());
		Branch sortedBranch = new Branch();
		sortedBranch.addAll(branchCopy);
		return sortedBranch;
	}
	
    private class DatabaseVersionHeaderComparator implements Comparator<DatabaseVersionHeader> {

        @Override
        public int compare(DatabaseVersionHeader o1, DatabaseVersionHeader o2) {
            VectorClockComparison vectorClockComparison = VectorClock.compare(o1.getVectorClock(), o2.getVectorClock());
           
            if (vectorClockComparison == VectorClockComparison.SIMULTANEOUS) {
                throw new RuntimeException("There must not be a conflict within a branch.");
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
	
	public Branch findLosersPruneBranch(Branch losersBranch, Branch winnersBranch) {
		Branch losersPruneBranch = new Branch();
		
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

	public Branch findWinnersApplyBranch(Branch losersBranch, Branch winnersBranch) {
		Branch winnersApplyBranch = new Branch();
		
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
