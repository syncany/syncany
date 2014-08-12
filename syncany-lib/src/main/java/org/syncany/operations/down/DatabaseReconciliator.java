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
 *  <li>{@link #findWinnersLastDatabaseVersionHeader(TreeMap, DatabaseBranches) findWinnersWinnersLastDatabaseVersionHeader()}:
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
	private static final Logger logger = Logger.getLogger(DatabaseReconciliator.class.getSimpleName());

	/**
	 * Implements the core synchronization algorithm as described {@link DatabaseReconciliator in the class description}.
	 * 
	 * @param localMachineName Client name of the local machine (required for branch stitching)
	 * @param localBranch Local branch, created from the local database
	 * @param unknownRemoteBranches Newly downloaded unknown remote branches (incomplete branches; will be stitched)
	 * @return Returns the branch of the winning client
	 */
	public Map.Entry<String, DatabaseBranch> findWinnerBranch(String localMachineName, DatabaseBranch localBranch, DatabaseBranches allStitchedBranches)
			throws Exception {
		
		DatabaseVersionHeader lastCommonHeader = findLastCommonDatabaseVersionHeader(localBranch, allStitchedBranches);
		TreeMap<String, DatabaseVersionHeader> firstConflictHeaders = findFirstConflictingDatabaseVersionHeader(lastCommonHeader, allStitchedBranches);
		TreeMap<String, DatabaseVersionHeader> winningFirstConflictHeaders = findWinningFirstConflictingDatabaseVersionHeaders(firstConflictHeaders);
		Entry<String, DatabaseVersionHeader> winnersLastHeader = findWinnersLastDatabaseVersionHeader(winningFirstConflictHeaders, allStitchedBranches);

		String winnersName = winnersLastHeader.getKey();
		DatabaseBranch winnersBranch = allStitchedBranches.getBranch(winnersName);

		if (logger.isLoggable(Level.FINEST)) {
			// TODO [low] Format this output nicer; This produces very, very, very long lines after a while
			logger.log(Level.FINEST, "- Database reconciliation results:");
			logger.log(Level.FINEST, "  + localBranch: " + localBranch);
			logger.log(Level.FINEST, "  + lastCommonHeader: " + lastCommonHeader);
			logger.log(Level.FINEST, "  + firstConflictingHeaders: " + firstConflictHeaders);
			logger.log(Level.FINEST, "  + winningFirstConflictingHeaders: " + winningFirstConflictHeaders);
			logger.log(Level.FINEST, "  + winnersWinnersLastDatabaseVersionHeader: " + winnersLastHeader);
		}

		if (logger.isLoggable(Level.INFO)) {
			logger.log(Level.INFO, "- Winner is " + winnersName + " with branch: ");

			for (DatabaseVersionHeader databaseVersionHeader : winnersBranch.getAll()) {
				logger.log(Level.INFO, "  + " + databaseVersionHeader);
			}
		}

		return new AbstractMap.SimpleEntry<String, DatabaseBranch>(winnersName, winnersBranch);
	}

	/**
	 * Finds the last common database version between a set of database branches
	 * of different clients. The purpose of finding the last common database version is
	 * to find the first conflicting database version (= last common + 1).
	 * 
	 * <p>This implementation checks whether each database version in the local branch
	 * is also contained in all of the given remoteBranches. For each database version
	 * header, {@link #isGreaterOrEqualDatabaseVersionHeaderInAllDatabaseBranches(VectorClock, DatabaseBranches) isGreaterOrEqualDatabaseVersionHeaderInAllDatabaseBranches()}
	 * is called. If the method returns true, the next database version header in the local
	 * branch is queried. If not, the last common database version header is the previous
	 * one.
	 * 
	 * @param localBranch Local branch (list database version headers) of this client
	 * @param remoteBranches All remote branches of the other clients
	 * @return Returns the last common database version header, or <tt>null</tt> if there is none
	 */
	// TODO [medium] This is very inefficient; Runtime O(n^3)!
	public DatabaseVersionHeader findLastCommonDatabaseVersionHeader(DatabaseBranch localBranch, DatabaseBranches remoteBranches) {
		DatabaseVersionHeader lastCommonDatabaseVersionHeader = null;

		for (DatabaseBranchIterator localBranchIterator = localBranch.iteratorLast(); localBranchIterator.hasPrevious();) {
			DatabaseVersionHeader currentLocalDatabaseVersionHeader = localBranchIterator.previous();

			if (isGreaterOrEqualDatabaseVersionHeaderInAllDatabaseBranches(currentLocalDatabaseVersionHeader, remoteBranches)) {
				lastCommonDatabaseVersionHeader = currentLocalDatabaseVersionHeader;
				break;
			}
		}

		return lastCommonDatabaseVersionHeader;
	}

	/**
	 * Checks if for all remote database branches, there exists at least one database version that is greater 
	 * or equal to the given database version. Returns <tt>true</tt> if there is, <tt>false</tt> otherwise.
	 * In other words: This method returns <tt>true</tt> if all the remote clients' database histories
	 * are based on the given database version. 
	 * 
	 * <p>If all remote branches are complete (first database version to last database version), checking for equality
	 * would be enough -- meaning that checking if the given database version is contained in all remote branches would be enough.
	 * However, due to the fact that we might have incomplete remote branches (e.g. only version (A5)-(A10) instead of (A1)-(A10)), 
	 * checking for greater and equal database versions is necessary.
	 * 
	 * <p>This method is used by 
	 * {@link #findLastCommonDatabaseVersionHeader(DatabaseBranch, DatabaseBranches) findLastCommonDatabaseVersionHeader()}
	 * to determine the last common database version between the local client and the given
	 * remote clients.
	 * 
	 * @param localDatabaseVersionHeader Local database version to check against the remote branches
	 * @param remoteDatabaseVersionHeaders List of database version of the remote clients 
	 * @return Returns <tt>true</tt> if the given vector clock is contained in all remote branches, <tt>false</tt> otherwise
	 */
	// TODO [medium] Do we still have to check for ">="? Isn't "=" enough? We should have full database branches here, because we stitch them before.
	private boolean isGreaterOrEqualDatabaseVersionHeaderInAllDatabaseBranches(DatabaseVersionHeader localDatabaseVersionHeader,
			DatabaseBranches remoteDatabaseVersionHeaders) {
		
		VectorClock localVectorClock = localDatabaseVersionHeader.getVectorClock();
		Set<String> remoteClients = remoteDatabaseVersionHeaders.getClients();

		for (String currentRemoteClient : remoteClients) {
			DatabaseBranch remoteBranch = remoteDatabaseVersionHeaders.getBranch(currentRemoteClient);
			boolean foundInCurrentClient = false;

			for (DatabaseVersionHeader remoteDatabaseVersionHeader : remoteBranch.getAll()) {
				VectorClock remoteVectorClock = remoteDatabaseVersionHeader.getVectorClock();
				VectorClockComparison remoteVsLocalVectorClockComparison = VectorClock.compare(remoteVectorClock, localVectorClock);

				if (remoteVsLocalVectorClockComparison == VectorClockComparison.GREATER
						|| remoteVsLocalVectorClockComparison == VectorClockComparison.EQUAL) {

					foundInCurrentClient = true;
					break;
				}
			}

			if (!foundInCurrentClient) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Finds the first conflicting database version per client. The first conflicting database version
	 * is the version after the last common database version (basically: last common + 1).
	 * 
	 * <p>The first conflicting database version per client is needed to decide the winner of the first
	 * conflict. This is later done based on the timestamp.
	 * 
	 * <p>The algorithm traverses each client's branch forward and compares the current database version
	 * header to the given last common header. If they match, the next database version header is
	 * assumed to be the first conflicting database version header -- even if it does not actually 
	 * conflict.
	 * 
	 * @param lastCommonHeader Last common database version header (as previously determined)
	 * @param allDatabaseBranches All database branches (remote and local), completely stitched
	 * @return Returns a per-client map (key) of the first conflicting database version header (value) 
	 */
	// TODO [medium] We have full branches (through stitching), can't we just walk forwards (like winner's winner comparison)?
	public TreeMap<String, DatabaseVersionHeader> findFirstConflictingDatabaseVersionHeader(DatabaseVersionHeader lastCommonHeader,
			DatabaseBranches allDatabaseBranches) {

		TreeMap<String, DatabaseVersionHeader> firstConflictingDatabaseVersionHeaders = new TreeMap<String, DatabaseVersionHeader>();

		nextClient: for (String remoteMachineName : allDatabaseBranches.getClients()) {
			DatabaseBranch remoteMachineBranch = allDatabaseBranches.getBranch(remoteMachineName);

			for (Iterator<DatabaseVersionHeader> i = remoteMachineBranch.iteratorFirst(); i.hasNext();) {
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
			if (remoteMachineBranch.size() > 0) {
				DatabaseVersionHeader firstConflictingInBranch = remoteMachineBranch.get(0);
				firstConflictingDatabaseVersionHeaders.put(remoteMachineName, firstConflictingInBranch);
			}
		}

		return firstConflictingDatabaseVersionHeaders;
	}

	/**
	 * Determines the first winning conflicting database version header per client, i.e. the database version headers
	 * that "win" the potential conflicts.
	 * 
	 * <p>After the first conflicting database versions have been found using 
	 * {@link #findFirstConflictingDatabaseVersionHeader(DatabaseVersionHeader, DatabaseBranches) findFirstConflictingDatabaseVersionHeader()},
	 * the winner among these database version headers must be found in order to determine the absolute winning branch.
	 * It is not uncommon that there are multiple winning first conflicting headers (e.g. two clients in sync; one
	 * client with later conflict).
	 * 
	 * <p>To determine the winner(s), all first conflicting headers are compared, and the earliest one (timestamp comparison) is
	 * picked as the winner (1). Then, the actual entries (client name to database version header) are selected (2). 
	 * 
	 * @param firstConflictingDatabaseVersionHeaders Per-client map of first conflicting database version headers
	 * @return Returns a map of per-client winning first conflicting database version headers. Key is client name, value 
	 *         is first conflicting database version header.
	 */
	public TreeMap<String, DatabaseVersionHeader> findWinningFirstConflictingDatabaseVersionHeaders(
			TreeMap<String, DatabaseVersionHeader> firstConflictingDatabaseVersionHeaders) {
		
		DatabaseVersionHeader winningFirstConflictingDatabaseVersionHeader = null;

		// (1) Compare all first conflicting ones and take the one with the EARLIEST timestamp
		for (DatabaseVersionHeader databaseVersionHeader : firstConflictingDatabaseVersionHeaders.values()) {
			if (winningFirstConflictingDatabaseVersionHeader == null) {
				winningFirstConflictingDatabaseVersionHeader = databaseVersionHeader;
			}
			else if (databaseVersionHeader.getDate().before(winningFirstConflictingDatabaseVersionHeader.getDate())) {
				winningFirstConflictingDatabaseVersionHeader = databaseVersionHeader;
			}
		}

		// (2) Find all first conflicting entries with the SAME timestamp as the
		// EARLIEST one (= multiple winning entries possible)
		TreeMap<String, DatabaseVersionHeader> winningFirstConflictingDatabaseVersionHeaders = new TreeMap<String, DatabaseVersionHeader>();

		for (Map.Entry<String, DatabaseVersionHeader> entry : firstConflictingDatabaseVersionHeaders.entrySet()) {
			if (winningFirstConflictingDatabaseVersionHeader.equals(entry.getValue())) {
				winningFirstConflictingDatabaseVersionHeaders.put(entry.getKey(), entry.getValue());
			}
		}

		return winningFirstConflictingDatabaseVersionHeaders;
	}

	/**
	 * Algorithm to find the ultimate winner's last database version header (client name and header).
	 * The ultimate winner's branch is used to determine the local file system actions.
	 * 
	 * <p>Basic algorithm: Iterate over all machines' branches forward, find conflicts and
	 * decide who wins. The following numbers correspond to the comments in the code
	 * 
	 * <ol>
	 *  <li>The algorithm first checks whether a winner comparison is even necessary. If there is only one
	 *      machine, it simply returns this machine as the winner.</li>
	 *  <li>If there is more than one machine, it determines per-client start positions in a branch. The start
	 *      position is the position at which the first conflicting database version was found (given as input
	 *      parameter).</li>
	 *  <li>It then starts a 'race' in which the database version headers of two machines are compared. If the
	 *      two database version headers are equal, both machines are left in the race. If they are not equal,
	 *      only the 'winner' stays in the race. This is repeated for each position of the machines' branches.
	 *      See the example below for a more graphic representation.</li>
	 *  <li>Once only one winner remains, the winner's name and its last database version header is
	 *      returned.</li>
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
	 * The algorithm input will be the database version headers in line 1 (= first conflicting
	 * database version headers). In the first step, the algorithm will get the positions per branch
	 * for the first conflicting database version headers. Here, this is A[1], B[1] and C[1].
	 * 
	 * <p>It will then compare the database version headers in the following order:
	 * <pre>
	 * Positions       1st machine       2nd machine
	 * -----------------------------------------------------------------------------
	 * Round 1:
	 * A[1] vs. B[1]   A: A/(A2)/T=13    B: A/(A2)/T=13      // Equal, no eliminations
	 * A[1] vs. C[1]   A: A/(A2)/T=13    C: C/(A1,C1)/T=14   // 13<14, A wins, eliminate C
	 * 
	 * Round 2 (C eliminated):
	 * A[2] vs. B[1]   A: A/(A3)/T=19    B: A/(A3)/T=19      // Equal, no eliminations
	 *      
	 * Round 3:
	 * A[3] vs. B[3]   A: A/(A4)/T=23    B: B/(A3,B1)/T=20   // 20<23, B wins, eliminate A
	 * 
	 * // B wins!
	 * </pre>
	 * 
	 * @param winningFirstConflictingDatabaseVersionHeaders Machine names and their corresponding first conflicting database version headers
	 * @param allBranches All stitched branches of all machines (including local)
	 * @return Returns the name and the last database version header of the winning machine 
	 */
	// TODO [low] I have not been able to significantly simplify this algorithm. Its semantics are pretty good, but the implementation is still not very nice.
	public Map.Entry<String, DatabaseVersionHeader> findWinnersLastDatabaseVersionHeader(
			TreeMap<String, DatabaseVersionHeader> winningFirstConflictingDatabaseVersionHeaders, DatabaseBranches allBranches)
			throws Exception {

		// 1. If there is only one conflicting database version header, return it (no need for a complex algorithm)
		if (winningFirstConflictingDatabaseVersionHeaders.size() == 1) {
			String winningMachineName = winningFirstConflictingDatabaseVersionHeaders.firstKey();
			DatabaseVersionHeader winnersWinnersLastDatabaseVersionHeader = allBranches.getBranch(winningMachineName).getLast();

			return new AbstractMap.SimpleEntry<String, DatabaseVersionHeader>(winningMachineName, winnersWinnersLastDatabaseVersionHeader);
		}

		// 2. Find position of first conflicting header in branch (per client)
		Map<String, Integer> machineInBranchPosition = findWinningFirstConflictDatabaseVersionHeaderPerClientPosition(
				winningFirstConflictingDatabaseVersionHeaders, allBranches);

		// 3. Compare all, go forward if all are identical
		int machineInRaceCount = winningFirstConflictingDatabaseVersionHeaders.size();		
		
		while (machineInRaceCount > 1) {
			String firstMachineName = null;
			DatabaseVersionHeader firstMachineDatabaseVersionHeader = null;

			for (Map.Entry<String, Integer> secondMachineNamePositionEntry : machineInBranchPosition.entrySet()) {
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
				DatabaseBranch secondMachineBranch = allBranches.getBranch(secondMachineName);

				if (secondMachinePosition >= secondMachineBranch.size()) {
					machineInBranchPosition.put(secondMachineName, null);
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
							machineInBranchPosition.put(firstMachineName, null);
							machineInRaceCount--;

							firstMachineName = secondMachineName;
							firstMachineDatabaseVersionHeader = secondMachineDatabaseVersionHeader;
						}
						else {
							machineInBranchPosition.put(secondMachineName, null);
							machineInRaceCount--;
						}
					}
				}
			}

			// 3c. If more than one machine are still in the race, increase positions
			if (machineInRaceCount > 1) {
				increaseBranchPosition(machineInBranchPosition);				
			}
		}

		// 4. Return the last remaining machine and its last database version header (= winner!)
		for (String machineName : machineInBranchPosition.keySet()) {
			Integer machineCurrentPosition = machineInBranchPosition.get(machineName);

			if (machineCurrentPosition != null) {
				DatabaseVersionHeader winnersWinnersLastDatabaseVersionHeader = allBranches.getBranch(machineName).getLast();
				return new AbstractMap.SimpleEntry<String, DatabaseVersionHeader>(machineName, winnersWinnersLastDatabaseVersionHeader);
			}
		}

		return null;
	}

	/**
	 * This method increases per-machine positions. 
	 * It ignores 'eliminated' machines (= machines with position 'null'). 
	 * 
	 * <p>This algorithm is part of the 
	 * {@link #findWinnersLastDatabaseVersionHeader(TreeMap, DatabaseBranches)} algorithm.
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
	 * {@link #findWinnersLastDatabaseVersionHeader(TreeMap, DatabaseBranches)} algorithm.
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

	/**
	 * Determines the position in client's branches at which the first conflicting database
	 * version header was found. This position is needed for the winner determination algorithm,
	 * which walks forwards through the branches.
	 * 
	 * <p>The algorithm walks forwards through each client branch and looks for the first
	 * conflicting header (as given in the parameter).
	 * 
	 * @param winningFirstConflictingDatabaseVersionHeaders First conflicting headers per client
	 * @param allDatabaseVersionHeaders All fully stitched branches of all clients (including local)
	 * @return Returns a per-client map of the array positions of the first conflicting headers per client
	 */
	private Map<String, Integer> findWinningFirstConflictDatabaseVersionHeaderPerClientPosition(
			TreeMap<String, DatabaseVersionHeader> winningFirstConflictingDatabaseVersionHeaders, DatabaseBranches allDatabaseVersionHeaders) {
		
		Map<String, Integer> machineBranchPositionIterator = new HashMap<String, Integer>();

		for (String machineName : winningFirstConflictingDatabaseVersionHeaders.keySet()) {
			DatabaseVersionHeader machineFirstConflictingDatabaseVersionHeader = winningFirstConflictingDatabaseVersionHeaders.get(machineName);
			DatabaseBranch machineBranch = allDatabaseVersionHeaders.getBranch(machineName);

			for (int i = 0; i < machineBranch.size(); i++) {
				DatabaseVersionHeader machineDatabaseVersionHeader = machineBranch.get(i);

				if (machineFirstConflictingDatabaseVersionHeader.equals(machineDatabaseVersionHeader)) {
					machineBranchPositionIterator.put(machineName, i);
					break;
				}
			}
		}

		return machineBranchPositionIterator;
	}

	public DatabaseBranches stitchBranches(DatabaseBranches unstitchedUnknownBranches, String localClientName, DatabaseBranch localBranch) {
		DatabaseBranches allBranches = unstitchedUnknownBranches.clone();

		mergeLocalBranchInRemoteBranches(localClientName, allBranches, localBranch);

		Set<DatabaseVersionHeader> allHeaders = gatherAllDatabaseVersionHeaders(allBranches);

		completeBranchesWithDatabaseVersionHeaders(allBranches, allHeaders);

		return allBranches;
	}

	private void mergeLocalBranchInRemoteBranches(String localClientName, DatabaseBranches allBranches, DatabaseBranch localBranch) {
		if (allBranches.getClients().contains(localClientName)) {
			DatabaseBranch unknownLocalClientBranch = allBranches.getBranch(localClientName);

			for (DatabaseVersionHeader header : localBranch.getAll()) {
				if (unknownLocalClientBranch.get(header.getVectorClock()) == null) {
					unknownLocalClientBranch.add(header);
				}
			}

			DatabaseBranch sortedClientBranch = sortBranch(unknownLocalClientBranch);
			allBranches.put(localClientName, sortedClientBranch);
		}
		else if (localBranch.size() > 0) {
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
			if (clientBranch.size() > 0) {
				VectorClock lastVectorClock = clientBranch.getLast().getVectorClock();

				for (DatabaseVersionHeader databaseVersionHeader : allHeaders) {
					VectorClock currentVectorClock = databaseVersionHeader.getVectorClock();
					boolean isCurrentVectorClockSmaller = VectorClock.compare(currentVectorClock, lastVectorClock) == VectorClockComparison.SMALLER;
					boolean currentVectorClockExistsInBranch = clientBranch.get(currentVectorClock) != null;
					boolean isInConflict = VectorClock.compare(lastVectorClock, currentVectorClock) == VectorClockComparison.SIMULTANEOUS;

					if (!currentVectorClockExistsInBranch && isCurrentVectorClockSmaller && !isInConflict) {
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
				throw new RuntimeException("There must not be a conflict within a branch. VC1: " + o1.getVectorClock() + " - VC2: "
						+ o2.getVectorClock());
			}

			if (vectorClockComparison == VectorClockComparison.EQUAL) {
				return 0;
			}
			else if (vectorClockComparison == VectorClockComparison.SMALLER) {
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
}
