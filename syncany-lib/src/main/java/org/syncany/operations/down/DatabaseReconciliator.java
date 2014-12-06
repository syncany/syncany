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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.VectorClock;

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
 * <p><b>Algorithm:</b>
 * <ol>
 *  <li>Input: Local branch, unknown remote branches</li>
 *  <li>Sort the databaseversions by vectorclocks, tiebreaking with timestamps.</li>
 *  <li>Walk through the sorted list and construct the winning branch.
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
	 * Implements the core synchronization algorithm as described {@link DatabaseReconciliator in the class description}.
	 * 
	 * @param localMachineName Client name of the local machine (required for branch stitching)
	 * @param localBranch Local branch, created from the local database
	 * @param unknownRemoteBranches Newly downloaded unknown remote branches (incomplete branches; will be stitched)
	 * @return Returns the branch of the winning client
	 */
	public Map.Entry<String, DatabaseBranch> findWinnerBranch(DatabaseBranches allBranches)
			throws Exception {

		Entry<String, DatabaseBranch> winnersNameAndBranch = findWinnersNameAndBranch(allBranches);

		if (winnersNameAndBranch != null) {
			String winnersName = winnersNameAndBranch.getKey();
			DatabaseBranch winnersBranch = winnersNameAndBranch.getValue();

			if (logger.isLoggable(Level.INFO)) {
				logger.log(Level.INFO, "- Winner is " + winnersName + " with branch: ");

				for (DatabaseVersionHeader databaseVersionHeader : winnersBranch.getAll()) {
					logger.log(Level.INFO, "  + " + databaseVersionHeader);
				}
			}

			return winnersNameAndBranch;
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
		logger.log(Level.INFO, "Finding winnersApplyBranch.");
		logger.log(Level.INFO, "Losers Branch: " + losersBranch);
		logger.log(Level.INFO, "Winners Branch: " + winnersBranch);
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
	private Entry<String, DatabaseBranch> findWinnersNameAndBranch(DatabaseBranches allBranches) {
		List<DatabaseVersionHeader> databaseVersionHeaders = sortBranches(allBranches);
		if (databaseVersionHeaders.size() == 0) {
			return null;
		}
		DatabaseBranch winnersBranch = new DatabaseBranch();
		DatabaseVersionHeaderComparator dbvComparator = new DatabaseVersionHeaderComparator(false);

		for (DatabaseVersionHeader potentialWinner : databaseVersionHeaders) {

			if (winnersBranch.size() == 0 || dbvComparator.compare(potentialWinner, winnersBranch.getLast()) > 0) {
				logger.log(Level.INFO, "Adding database version to winning branch: " + potentialWinner);
				winnersBranch.add(potentialWinner);
			}
			else {
				logger.log(Level.INFO, "Ignoring databaseVersion: " + potentialWinner);
			}
		}

		DatabaseVersionHeader winningDatabaseVersionHeader = winnersBranch.getLast();

		for (String client : allBranches.getClients()) {
			if (winningDatabaseVersionHeader.equals(allBranches.getBranch(client).getLast())) {
				return new AbstractMap.SimpleEntry<String, DatabaseBranch>(client, winnersBranch);
			}
		}

		return null;
	}

	private List<DatabaseVersionHeader> sortBranches(DatabaseBranches allBranches) {
		List<DatabaseVersionHeader> databaseVersionHeaders = new ArrayList<DatabaseVersionHeader>();
		for (String client : allBranches.getClients()) {
			databaseVersionHeaders.addAll(allBranches.getBranch(client).getAll());
		}
		Collections.sort(databaseVersionHeaders, new DatabaseVersionHeaderComparator(true));

		return databaseVersionHeaders;
	}
}
