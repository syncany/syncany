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

import java.util.Comparator;

import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.VectorClock;
import org.syncany.database.VectorClock.VectorClockComparison;

/**
 * Comparator to be used when comparing {@link DatabaseVersionHeader}s. The comparison precedence
 * is as follows, if the comparator is time sensitive.
 * 
 * <ol>
 *  <li>Comparison by {@link VectorClock}</li>
 *  <li>Comparison by timestamp/date</li>
 *  <li>Comparison by name of the client</li>
 * </ol>
 *
 * If the comparator is not time sensitive, the ordering is specified solely by the {@link VectorClock}s.
 * Larger and smaller {@link VectorClock}s result in larger, respectively smaller {@link DatabaseVersionHeader}s.
 * Equal and simultaneous {@link VectorClock}s result in equal {@link DatabaseVersionHeader}s. Note that in
 * this case the name of the client is not used either.
 * 
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
public class DatabaseVersionHeaderComparator implements Comparator<DatabaseVersionHeader> {
	private boolean considerTime;

	public DatabaseVersionHeaderComparator(boolean considerTime) {
		this.considerTime = considerTime;
	}

	/**
	 * Compares the two given database versions headers and returns -1, 0 or 1 depending on
	 * which header is considered larger. See {@link DatabaseVersionHeaderComparator class description}
	 * for details regarding the precedence.
	 *
	 * @return -1 if dbvh1 is smaller than dbvh2, 0 if they are equal, 1 if dbvh1 is greater than dbvh2
	 */
	@Override
	public int compare(DatabaseVersionHeader dbvh1, DatabaseVersionHeader dbvh2) {
		return compareByVectorClock(dbvh1, dbvh2);
	}

	private int compareByVectorClock(DatabaseVersionHeader dbvh1, DatabaseVersionHeader dbvh2) {
		VectorClockComparison vectorClockComparison = VectorClock.compare(dbvh1.getVectorClock(), dbvh2.getVectorClock());
		
		if (vectorClockComparison == VectorClockComparison.SIMULTANEOUS || vectorClockComparison == VectorClockComparison.EQUAL) {
			if (considerTime) {
				return compareByTimestamp(dbvh1, dbvh2);
			}
			else {
				return 0;
			}
		}

		return vectorClockComparison == VectorClockComparison.SMALLER ? -1 : 1;
	}

	private int compareByTimestamp(DatabaseVersionHeader dbvh1, DatabaseVersionHeader dbvh2) {
		int timestampComparison = Long.compare(dbvh1.getDate().getTime(), dbvh2.getDate().getTime());

		if (timestampComparison == 0) {
			return compareByClientName(dbvh1, dbvh2);
		}
		else {
			return timestampComparison;
		}
	}

	private int compareByClientName(DatabaseVersionHeader dbvh1, DatabaseVersionHeader dbvh2) {
		return dbvh1.getClient().compareTo(dbvh2.getClient());
	}
}