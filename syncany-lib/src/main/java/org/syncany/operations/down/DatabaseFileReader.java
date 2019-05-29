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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.VectorClock;
import org.syncany.database.dao.DatabaseXmlSerializer;
import org.syncany.database.dao.DatabaseXmlSerializer.DatabaseReadType;

/**
 * The DatabaseFileReader provides a way to read a series of database files
 * in a memory-efficient way, by converting them to a series of MemoryDatabases,
 * none of which are too large.
 * 
 * @author Pim Otte
 */
public class DatabaseFileReader implements Iterator<MemoryDatabase> {
	private static final int MAX_FILES = 9999;

	private DatabaseXmlSerializer databaseSerializer;
	private List<DatabaseVersionHeader> winnersApplyBranchList;
	private Map<DatabaseVersionHeader, File> databaseVersionLocations;
	private int branchIndex = 0;

	public DatabaseFileReader(DatabaseXmlSerializer databaseSerializer, DatabaseBranch winnersApplyBranch,
			Map<DatabaseVersionHeader, File> databaseVersionLocations) {
		
		this.winnersApplyBranchList = winnersApplyBranch.getAll();
		this.databaseVersionLocations = databaseVersionLocations;
		this.databaseSerializer = databaseSerializer;
	}

	public boolean hasNext() {
		return branchIndex < winnersApplyBranchList.size();
	}

	/**
	 * Loads the winner's database branch into the memory in a {@link MemoryDatabase} object, by using
	 * the already downloaded list of remote database files.
	 *
	 * <p>Because database files can contain multiple {@link DatabaseVersion}s per client, a range for which
	 * to load the database versions must be determined.
	 *
	 * <p><b>Example 1:</b><br>
	 * <pre>
	 *  db-A-0001   (A1)     Already known             Not loaded
	 *  db-A-0005   (A2)     Already known             Not loaded
	 *              (A3)     Already known             Not loaded
	 *              (A4)     Part of winner's branch   Loaded
	 *              (A5)     Purge database version    Ignored (only DEFAULT)
	 *  db-B-0001   (A5,B1)  Part of winner's branch   Loaded
	 *  db-A-0006   (A6,B1)  Part of winner's branch   Loaded
	 * </pre>
	 *
	 * <p>In example 1, only (A4)-(A5) must be loaded from db-A-0005, and not all four database versions.
	 *
	 * <p><b>Other example:</b><br>
	 * <pre>
	 *  db-A-0005   (A1)     Part of winner's branch   Loaded
	 *  db-A-0005   (A2)     Part of winner's branch   Loaded
	 *  db-B-0001   (A2,B1)  Part of winner's branch   Loaded
	 *  db-A-0005   (A3,B1)  Part of winner's branch   Loaded
	 *  db-A-0005   (A4,B1)  Part of winner's branch   Loaded
	 *  db-A-0005   (A5,B1)  Purge database version    Ignored (only DEFAULT)
	 * </pre>
	 *
	 * <p>In example 2, (A1)-(A5,B1) [except (A2,B1)] are contained in db-A-0005 (after merging!), so
	 * db-A-0005 must be processed twice; each time loading separate parts of the file. In this case:
	 * First load (A1)-(A2) from db-A-0005, then load (A2,B1) from db-B-0001, then load (A3,B1)-(A4,B1)
	 * from db-A-0005, and ignore (A5,B1).
	 *
	 * @return Returns a loaded memory database containing all metadata from the winner's branch
	 */
	@Override
	public MemoryDatabase next() {
		MemoryDatabase winnerBranchDatabase = new MemoryDatabase();
		String rangeClientName = null;
		VectorClock rangeVersionFrom = null;
		VectorClock rangeVersionTo = null;

		while (branchIndex < winnersApplyBranchList.size() && winnerBranchDatabase.getFileHistories().size() < MAX_FILES) {
			DatabaseVersionHeader currentDatabaseVersionHeader = winnersApplyBranchList.get(branchIndex);
			DatabaseVersionHeader nextDatabaseVersionHeader = (branchIndex + 1 < winnersApplyBranchList.size()) ? winnersApplyBranchList
					.get(branchIndex + 1) : null;

			// First of range for this client
			if (rangeClientName == null) {
				rangeClientName = currentDatabaseVersionHeader.getClient();
				rangeVersionFrom = currentDatabaseVersionHeader.getVectorClock();
				rangeVersionTo = currentDatabaseVersionHeader.getVectorClock();
			}

			// Still in range for this client
			else {
				rangeVersionTo = currentDatabaseVersionHeader.getVectorClock();
			}

			// Now load this stuff from the database file (or not)
			//   - If the database file exists, load the range and reset it
			//   - If not, only force a load if this is the range end

			File databaseVersionFile = databaseVersionLocations.get(currentDatabaseVersionHeader);

			if (databaseVersionFile == null) {
				throw new RuntimeException("Could not find file corresponding to " + currentDatabaseVersionHeader
						+ ", while it is in the winners branch.");
			}

			boolean lastDatabaseVersionHeader = nextDatabaseVersionHeader == null;
			boolean nextDatabaseVersionInSameFile = lastDatabaseVersionHeader
					|| databaseVersionFile.equals(databaseVersionLocations.get(nextDatabaseVersionHeader));
			boolean rangeEnds = lastDatabaseVersionHeader || !nextDatabaseVersionInSameFile;

			if (rangeEnds) {
				try {
					databaseSerializer.load(winnerBranchDatabase, databaseVersionFile, rangeVersionFrom, rangeVersionTo, DatabaseReadType.FULL);
				}
				catch (IOException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
				rangeClientName = null;
			}
			branchIndex++;
		}

		return winnerBranchDatabase;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Removing a databaseversion is not supported");

	}

}
