package org.syncany.operations;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.syncany.database.Branch;
import org.syncany.database.Branches;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.VectorClock;
import org.syncany.database.Branch.BranchIterator;
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
 *   - TODO TALK ABOUT THE NEXT PARTS
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
 *    - TODO Make an algorithm that constructs the new local history
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
public class DatabaseVersionUpdateDetector {
	private static final Logger logger = Logger.getLogger(DatabaseVersionUpdateDetector.class.getSimpleName());

	@Deprecated
	public DatabaseVersionHeader findLastCommonDatabaseVersionHeader(TreeMap<Long, DatabaseVersionHeader> localDatabaseVersionHeaders,
			TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> remoteDatabaseVersionHeaders) {
		DatabaseVersionHeader lastCommonDatabaseVersionHeader = null;

		for (Long currentLocalDatabaseKey = localDatabaseVersionHeaders.lastKey(); currentLocalDatabaseKey != null
				&& lastCommonDatabaseVersionHeader == null; currentLocalDatabaseKey = localDatabaseVersionHeaders.lowerKey(currentLocalDatabaseKey)) {
			DatabaseVersionHeader currentLocalDatabaseVersionHeader = localDatabaseVersionHeaders.get(currentLocalDatabaseKey);
			VectorClock currentVectorClock = currentLocalDatabaseVersionHeader.getVectorClock();

			if (isKeyInAllRemoteDatabasesGreaterOrEqual(currentVectorClock, remoteDatabaseVersionHeaders)) {
				lastCommonDatabaseVersionHeader = currentLocalDatabaseVersionHeader;
			}
		}

		return lastCommonDatabaseVersionHeader;
	}
	
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

	@Deprecated
	private boolean isKeyInAllRemoteDatabasesGreaterOrEqual(VectorClock currentVectorClock,
			TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> remoteDatabaseVersionHeaders) {
		Set<String> clients = remoteDatabaseVersionHeaders.keySet();
		Map<String, Boolean> foundInClientMatrix = initializeFoundInClientMatrix(clients);

		for (Map.Entry<String, TreeMap<Long, DatabaseVersionHeader>> remoteClientDatabaseVersionsSet : remoteDatabaseVersionHeaders.entrySet()) {
			String currentRemoteClient = remoteClientDatabaseVersionsSet.getKey();
			TreeMap<Long, DatabaseVersionHeader> remoteClientDatabaseVersions = remoteClientDatabaseVersionsSet.getValue();

			for (DatabaseVersionHeader remoteDatabaseVersionHeader : remoteClientDatabaseVersions.values()) {
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

	public TreeMap<String, DatabaseVersionHeader> findFirstConflictingDatabaseVersionHeader(DatabaseVersionHeader lastCommonHeader, String localName,
			Branch localDatabaseVersionHeaders, Branches remoteDatabaseVersionHeaders) {
		TreeMap<String, DatabaseVersionHeader> firstConflictingDatabaseVersionHeaders = new TreeMap<String, DatabaseVersionHeader>();

		boolean next = false;
		for (DatabaseVersionHeader databaseVersionHeader : localDatabaseVersionHeaders.getAll()) {
			if (next) {
				firstConflictingDatabaseVersionHeaders.put(localName, databaseVersionHeader);
				break;
			} else if (databaseVersionHeader.equals(lastCommonHeader)) {
				next = true;
			}
		}

		for (String remoteMachineName : remoteDatabaseVersionHeaders.getClients()) {
			Branch remoteBranch = remoteDatabaseVersionHeaders.getBranch(remoteMachineName);

			boolean next2 = false;
			for (DatabaseVersionHeader databaseVersionHeader : remoteBranch.getAll()) {
				if (next2) {
					firstConflictingDatabaseVersionHeaders.put(remoteMachineName, databaseVersionHeader);
					break;
				} else if (databaseVersionHeader.equals(lastCommonHeader)) {
					next2 = true;
				}
			}

			if (next2 == false) { // Nothing found. Add first as conflict
				firstConflictingDatabaseVersionHeaders.put(remoteMachineName, remoteBranch.getFirst());
			}
		}

		return firstConflictingDatabaseVersionHeaders;
	}

	@Deprecated
	public TreeMap<String, DatabaseVersionHeader> findFirstConflictingDatabaseVersionHeader(DatabaseVersionHeader lastCommonHeader, String localName,
			TreeMap<Long, DatabaseVersionHeader> localDatabaseVersionHeaders,
			Map<String, TreeMap<Long, DatabaseVersionHeader>> remoteDatabaseVersionHeaders) {
		TreeMap<String, DatabaseVersionHeader> firstConflictingDatabaseVersionHeaders = new TreeMap<String, DatabaseVersionHeader>();

		boolean next = false;
		for (DatabaseVersionHeader databaseVersionHeader : localDatabaseVersionHeaders.values()) {
			if (next) {
				firstConflictingDatabaseVersionHeaders.put(localName, databaseVersionHeader);
				break;
			} else if (databaseVersionHeader.equals(lastCommonHeader)) {
				next = true;
			}
		}

		for (Map.Entry<String, TreeMap<Long, DatabaseVersionHeader>> entry : remoteDatabaseVersionHeaders.entrySet()) {
			String thisRemoteMachineName = entry.getKey();
			TreeMap<Long, DatabaseVersionHeader> thisRemoteDatabaseVersionHeaders = entry.getValue();

			boolean next2 = false;
			for (DatabaseVersionHeader databaseVersionHeader : thisRemoteDatabaseVersionHeaders.values()) {
				if (next2) {
					firstConflictingDatabaseVersionHeaders.put(thisRemoteMachineName, databaseVersionHeader);
					break;
				} else if (databaseVersionHeader.equals(lastCommonHeader)) {
					next2 = true;
				}
			}

			if (next2 == false) { // Nothing found. Add first as conflict
				firstConflictingDatabaseVersionHeaders.put(thisRemoteMachineName, thisRemoteDatabaseVersionHeaders.firstEntry().getValue());
			}
		}

		return firstConflictingDatabaseVersionHeaders;
	}

	public TreeMap<String, DatabaseVersionHeader> findWinningFirstConflictingDatabaseVersionHeaders(
			TreeMap<String, DatabaseVersionHeader> firstConflictingDatabaseVersionHeaders) {
		// TODO this method curently does not catch the scenario in which two
		// first winning conflict headers have the same timestamp, this could be
		// baaad, though very unlikely
		DatabaseVersionHeader winningFirstConflictingDatabaseVersionHeader = null;

		// Compare all first conflicting ones and take the one with the EARLIEST
		// timestamp
		for (DatabaseVersionHeader databaseVersionHeader : firstConflictingDatabaseVersionHeaders.values()) {
			if (winningFirstConflictingDatabaseVersionHeader == null) {
				winningFirstConflictingDatabaseVersionHeader = databaseVersionHeader;
			} else if (databaseVersionHeader.getUploadedDate().before(winningFirstConflictingDatabaseVersionHeader.getUploadedDate())) {
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
		// TODO ugly
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

				DatabaseVersionHeader currentMachineDatabaseVersionHeader = machineDatabaseVersionHeaders.get(machinePosition);

				if (currentComparisonDatabaseVersionHeader == null) {
					currentComparisonMachineName = machineName;
					currentComparisonDatabaseVersionHeader = currentMachineDatabaseVersionHeader;
				} else {
					VectorClockComparison comparison = VectorClock.compare(currentComparisonDatabaseVersionHeader.getVectorClock(),
							currentMachineDatabaseVersionHeader.getVectorClock());

					if (comparison != VectorClockComparison.EQUAL) {
						if (currentComparisonDatabaseVersionHeader.getUploadedDate().before(currentMachineDatabaseVersionHeader.getUploadedDate())) {
							// Eliminate machine in current loop

							machineBranchPositionIterator.put(machineName, null);
							machineInRaceCount--;
						} else if (currentMachineDatabaseVersionHeader.getUploadedDate().before(
								currentComparisonDatabaseVersionHeader.getUploadedDate())) {
							// Eliminate comparison machine

							machineBranchPositionIterator.put(currentComparisonMachineName, null);
							machineInRaceCount--;

							currentComparisonMachineName = machineName;
							currentComparisonDatabaseVersionHeader = currentMachineDatabaseVersionHeader;
						} else {
							throw new Exception("This should not happen."); // FIXME
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
	public Map.Entry<String, DatabaseVersionHeader> findWinnersWinnersLastDatabaseVersionHeader(
			TreeMap<String, DatabaseVersionHeader> winningFirstConflictingDatabaseVersionHeaders,
			TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> allDatabaseVersionHeaders) throws Exception {

		if (winningFirstConflictingDatabaseVersionHeaders.size() == 1) {
			String winningMachineName = winningFirstConflictingDatabaseVersionHeaders.firstKey();
			DatabaseVersionHeader winnersWinnersLastDatabaseVersionHeader = allDatabaseVersionHeaders.get(winningMachineName).lastEntry().getValue();

			return new AbstractMap.SimpleEntry<String, DatabaseVersionHeader>(winningMachineName, winnersWinnersLastDatabaseVersionHeader);
		}

		// Algorithm:
		// Iterate over all machines' branches forward, find conflicts and
		// decide who wins

		// 1. Find winners winners positions in branch
		Map<String, Long> machineBranchPositionIterator = new HashMap<String, Long>();

		for (String machineName : winningFirstConflictingDatabaseVersionHeaders.keySet()) {
			DatabaseVersionHeader machineWinnersWinner = winningFirstConflictingDatabaseVersionHeaders.get(machineName);
			TreeMap<Long, DatabaseVersionHeader> machineDatabaseVersionHeaders = allDatabaseVersionHeaders.get(machineName);

			for (Map.Entry<Long, DatabaseVersionHeader> e = machineDatabaseVersionHeaders.firstEntry(); e != null; e = machineDatabaseVersionHeaders
					.higherEntry(e.getKey())) {
				if (machineWinnersWinner.equals(e.getValue())) {
					machineBranchPositionIterator.put(machineName, e.getKey());
					break;
				}
			}
		}

		// 2. Compare all, go forward if all are identical
		int machineInRaceCount = winningFirstConflictingDatabaseVersionHeaders.size();

		while (machineInRaceCount > 1) {
			String currentComparisonMachineName = null;
			DatabaseVersionHeader currentComparisonDatabaseVersionHeader = null;

			for (Map.Entry<String, Long> machineBranchPosition : machineBranchPositionIterator.entrySet()) {
				String machineName = machineBranchPosition.getKey();
				TreeMap<Long, DatabaseVersionHeader> machineDatabaseVersionHeaders = allDatabaseVersionHeaders.get(machineName);
				Long machinePosition = machineBranchPosition.getValue();

				if (machinePosition == null) {
					continue;
				}

				DatabaseVersionHeader currentMachineDatabaseVersionHeader = machineDatabaseVersionHeaders.get(machinePosition);

				if (currentComparisonDatabaseVersionHeader == null) {
					currentComparisonMachineName = machineName;
					currentComparisonDatabaseVersionHeader = currentMachineDatabaseVersionHeader;
				} else {
					VectorClockComparison comparison = VectorClock.compare(currentComparisonDatabaseVersionHeader.getVectorClock(),
							currentMachineDatabaseVersionHeader.getVectorClock());

					if (comparison != VectorClockComparison.EQUAL) {
						if (currentComparisonDatabaseVersionHeader.getUploadedDate().before(currentMachineDatabaseVersionHeader.getUploadedDate())) {
							// Eliminate machine in current loop

							machineBranchPositionIterator.put(machineName, null);
							machineInRaceCount--;
						} else if (currentMachineDatabaseVersionHeader.getUploadedDate().before(
								currentComparisonDatabaseVersionHeader.getUploadedDate())) {
							// Eliminate comparison machine

							machineBranchPositionIterator.put(currentComparisonMachineName, null);
							machineInRaceCount--;

							currentComparisonMachineName = machineName;
							currentComparisonDatabaseVersionHeader = currentMachineDatabaseVersionHeader;
						} else {
							throw new Exception("This should not happen."); // FIXME
						}
					}
				}
			}

			if (machineInRaceCount > 1) {
				for (String machineName : machineBranchPositionIterator.keySet()) {
					Long machineCurrentKey = machineBranchPositionIterator.get(machineName);

					if (machineCurrentKey != null) {
						Long machineHigherKey = allDatabaseVersionHeaders.get(machineName).higherKey(machineCurrentKey);
						machineBranchPositionIterator.put(machineName, machineHigherKey);
					}
				}
			}
		}

		for (String machineName : machineBranchPositionIterator.keySet()) {
			Long machineCurrentKey = machineBranchPositionIterator.get(machineName);

			if (machineCurrentKey != null) {
				DatabaseVersionHeader winnersWinnersLastDatabaseVersionHeader = allDatabaseVersionHeaders.get(machineName).lastEntry().getValue();
				return new AbstractMap.SimpleEntry<String, DatabaseVersionHeader>(machineName, winnersWinnersLastDatabaseVersionHeader);
			}
		}

		return null;
	}

	public Branches fillRemoteBranches(Branch localBranch, Branches remoteBranches) {
		Branches filledRemoteBranches = new Branches();

		for (String remoteClientName : remoteBranches.getClients()) {
			Branch remoteBranch = remoteBranches.getBranch(remoteClientName);
			Branch filledRemoteBranch = filledRemoteBranches.getBranch(remoteClientName, true);
			
			DatabaseVersionHeader firstRemoteDatabaseVersionHeader = remoteBranch.getFirst();
			if (firstRemoteDatabaseVersionHeader == null) {
				// Client in sync with local; why we have him listed as remote
				// new dbv?
				throw new RuntimeException("Client " + remoteClientName + " listed without any new DBV.");
			}

			VectorClock firstRemoteVectorClock = firstRemoteDatabaseVersionHeader.getVectorClock();

			for (DatabaseVersionHeader localDatabaseVersion : localBranch.getAll()) {
				VectorClock currentLocalVectorClock = localDatabaseVersion.getVectorClock();
				if (VectorClock.compare(firstRemoteVectorClock, currentLocalVectorClock) == VectorClockComparison.GREATER) {
					filledRemoteBranch.add(localDatabaseVersion);
				} 
			}
			
			filledRemoteBranch.addAll(remoteBranch.getAll());
		}

		return filledRemoteBranches;
	}
	
	@Deprecated
	public TreeMap<String, List<DatabaseVersionHeader>> orchestrateBranch(TreeMap<Long, DatabaseVersionHeader> localDatabaseVersionHeaders,
			TreeMap<String, TreeMap<Long, DatabaseVersionHeader>> remoteDatabaseVersionHeaders) {
		TreeMap<String, List<DatabaseVersionHeader>> orchestratedBranch = new TreeMap<String, List<DatabaseVersionHeader>>();

		for (Map.Entry<String, TreeMap<Long, DatabaseVersionHeader>> remoteClient : remoteDatabaseVersionHeaders.entrySet()) {
			String remoteClientName = remoteClient.getKey();
			TreeMap<Long, DatabaseVersionHeader> currentRemoteDatabaseVersions = remoteClient.getValue();
			List<DatabaseVersionHeader> currentDatabaseVersions = new ArrayList<DatabaseVersionHeader>(currentRemoteDatabaseVersions.values());
			List<DatabaseVersionHeader> orchestratedClientBranch = new ArrayList<DatabaseVersionHeader>();

			DatabaseVersionHeader firstRemoteDatabaseVersionHeader = currentDatabaseVersions.get(0);
			VectorClock firstRemoteVectorClock = firstRemoteDatabaseVersionHeader.getVectorClock();
			if (firstRemoteDatabaseVersionHeader == null) {
				// Client in sync with local; why we have him listed as remote
				// new dbv?
				throw new RuntimeException("Client " + remoteClientName + " listed without any new DBV.");
			}

			for (DatabaseVersionHeader localDatabaseVersion : localDatabaseVersionHeaders.values()) {
				VectorClock currentLocalVectorClock = localDatabaseVersion.getVectorClock();
				if (VectorClock.compare(firstRemoteVectorClock, currentLocalVectorClock) == VectorClockComparison.GREATER) {
					orchestratedClientBranch.add(localDatabaseVersion);
				} 
			}
			
			orchestratedClientBranch.addAll(currentDatabaseVersions);
			orchestratedBranch.put(remoteClientName, orchestratedClientBranch);
		}

		return orchestratedBranch;
	}

}
