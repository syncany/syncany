/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
 * is as follows:
 * 
 * <ol>
 *  <li>Comparison by {@link VectorClock}</li>
 *  <li>Comparison by timestamp/date</li>
 *  <li>Comparison by name of the client</li>
 * </ol>
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
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
	 */
	@Override
	public int compare(DatabaseVersionHeader o1, DatabaseVersionHeader o2) {
		return compareByVectorClock(o1, o2);
	}

	private int compareByVectorClock(DatabaseVersionHeader o1, DatabaseVersionHeader o2) {
		VectorClockComparison vectorClockComparison = VectorClock.compare(o1.getVectorClock(), o2.getVectorClock());
		
		if (vectorClockComparison == VectorClockComparison.SIMULTANEOUS) {
			if (considerTime) {
				return compareByTimestamp(o1, o2);
			}
			else {
				return 0;
			}
		}
		
		if (vectorClockComparison == VectorClockComparison.EQUAL) {
			return compareByTimestamp(o1, o2);
		}		
		else if (vectorClockComparison == VectorClockComparison.SMALLER) {
			return -1;
		}
		else {
			return 1;
		}
	}

	private int compareByTimestamp(DatabaseVersionHeader o1, DatabaseVersionHeader o2) {
		int timestampComparison = Long.compare(o1.getDate().getTime(), o2.getDate().getTime());

		if (timestampComparison == 0) {
			return compareByClientName(o1, o2);
		}
		else {
			return timestampComparison;
		}
	}

	private int compareByClientName(DatabaseVersionHeader o1, DatabaseVersionHeader o2) {
		return o1.getClient().compareTo(o2.getClient());
	}
}