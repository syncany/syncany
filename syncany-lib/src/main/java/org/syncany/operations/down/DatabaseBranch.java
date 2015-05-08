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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersionHeader;

/**
 * A branch represents a list of {@link DatabaseVersionHeader}s, thereby identifying a
 * the history of a client's database. It can be compared to a sorted list of
 * {@link DatabaseVersion} pointers.
 *
 * <p>Branches are used mainly in the {@link DatabaseReconciliator} to compare database
 * versions and reconcile conflicts.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class DatabaseBranch {
	private ArrayList<DatabaseVersionHeader> branch;

	public DatabaseBranch() {
		this.branch = new ArrayList<DatabaseVersionHeader>();
	}

	public void add(DatabaseVersionHeader header) {
		branch.add(header);
	}

	public void addAll(List<DatabaseVersionHeader> headers) {
		branch.addAll(headers);
	}

	public int size() {
		return branch.size();
	}

	public DatabaseVersionHeader get(int index) {
		if (index >= 0 && index < branch.size()) {
			return branch.get(index);
		}
		return null;
	}

	public List<DatabaseVersionHeader> getAll() {
		return Collections.unmodifiableList(branch);
	}

	public DatabaseVersionHeader getLast() {
		if (branch.size() == 0) {
			return null;
		}
		return branch.get(branch.size() - 1);
	}

	@Override
	public String toString() {
		return branch.toString();
	}

	@Override
	public DatabaseBranch clone() {
		DatabaseBranch clonedBranch = new DatabaseBranch();
		clonedBranch.addAll(getAll());

		return clonedBranch;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((branch == null) ? 0 : branch.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof DatabaseBranch)) {
			return false;
		}
		DatabaseBranch other = (DatabaseBranch) obj;
		if (branch == null) {
			if (other.branch != null) {
				return false;
			}
		}
		else if (!branch.equals(other.branch)) {
			return false;
		}
		return true;
	}
}
