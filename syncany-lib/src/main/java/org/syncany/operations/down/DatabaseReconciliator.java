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
 * @author Pim Otte <otte.pim@gmail.com>
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
	 * <p>Basic algorithm: Sort all databaseversions by vectorclocks, tiebreaking with timestamps and machinenames.
	 * Iterate over this list, adding databaseversions to the winning branch if they are not simultaneous with the
	 * winning branch up until this point. 
	 *
	 * <p><b>Illustration:</b><br />
	 * Suppose the following branches exist. 
	 * Naming: <em>created-by / vector clock / local time</em>.
	 * 
	 * <pre>
	 *    A               B                C
	 * --|-------------------------------------------------
	 * 0 | A/(A1)/T=10    B/(A3,B1)/T=20   C/(A1,C1)/T=14
	 * 1 | A/(A2)/T=13                     C/(A1,C2)/T=15
	 * 2 | A/(A3)/T=19          
	 * 3 | A/(A4)/T=23       
	 * </pre>
	 * 
	 * <b>Sorted Database versions:</b>
	 * <ol>
	 * 	<li>A[0]:A/(A1)/T=10</li>
	 *  <li>A[1]:A/(A2)/T=13</li>
	 *  <li>C[0]:C/(A1,C1)/T=14</li>
	 *  <li>C[1]:C/(A1,C2)/T=15</li>
	 *  <li>A[2]:A/(A3)/T=19</li>
	 *  <li>B[0]:B/(A3,B1)/T=20</li>
	 *  <li>A[3]:A/(A4)/T=23</li>
	 * </ol> 
	 * 
	 * <b>Iterating through the list:</b>
	 * <ol> 
	 *  <li>A[0] is the first version. Add it.</li>
	 *  <li>A[1] > A[0]. Add it.</li>
	 *  <li>C[0] is simultaneous with A[1]. Ignore it.</li>
	 *  <li>C[1] is simultaneous with A[1]. Ignore it.</li>
	 *  <li>A[2] > A[1]. Add it.</li>
	 *  <li>B[0] > A[2]. Add it.</li>
	 *  <li>A[3] is simultaneous with B[0]. Ignore it.</li>
	 * </ol>
	 *  
	 * <b>Winning branch:</b>
	 * <ol>
	 * 	<li>A[0]:A/(A1)/T=10</li>
	 *  <li>A[1]:A/(A2)/T=13</li>
	 *  <li>A[2]:A/(A3)/T=19</li>
	 *  <li>B[0]:B/(A3,B1)/T=20</li>
	 * </ol> 
	 * 
	 * Last version matches last version of B. Hence B wins.
	 * 
	 * @param allStitchedBranches All branches of all machines (including local)
	 * @return Returns the name and the branch of the winning machine 
	 */
	private Entry<String, DatabaseBranch> findWinnersNameAndBranch(DatabaseBranches allBranches) {
		List<DatabaseVersionHeader> databaseVersionHeaders = sortBranches(allBranches);
		
		if (databaseVersionHeaders.size() == 0) {
			return null;
		}
		
		// Determine winning branch
		DatabaseBranch winnersBranch = new DatabaseBranch();
		DatabaseVersionHeaderComparator databaseVersionHeaderComparator = new DatabaseVersionHeaderComparator(false);

		for (DatabaseVersionHeader potentialWinner : databaseVersionHeaders) {
			boolean emptyWinnerBranch = winnersBranch.size() == 0;
			boolean potentialWinnerWins = !emptyWinnerBranch && databaseVersionHeaderComparator.compare(potentialWinner, winnersBranch.getLast()) > 0;

			if (emptyWinnerBranch || potentialWinnerWins) {
				logger.log(Level.INFO, "Adding database version to winning branch: " + potentialWinner);
				winnersBranch.add(potentialWinner);
			}
			else {
				logger.log(Level.INFO, "Ignoring databaseVersion: " + potentialWinner);
			}
		}

		// Determine client name for winning branch
		DatabaseVersionHeader winningLastDatabaseVersionHeader = winnersBranch.getLast();

		for (String currentClient : allBranches.getClients()) {
			DatabaseBranch currentBranch = allBranches.getBranch(currentClient);
			DatabaseVersionHeader currentBranchLastDatabaseVersionHeader = currentBranch.getLast();
			
			if (winningLastDatabaseVersionHeader.equals(currentBranchLastDatabaseVersionHeader)) {
				return new AbstractMap.SimpleEntry<String, DatabaseBranch>(currentClient, winnersBranch);
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
