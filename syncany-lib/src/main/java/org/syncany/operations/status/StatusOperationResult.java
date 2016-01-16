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
package org.syncany.operations.status;

import org.syncany.operations.ChangeSet;
import org.syncany.operations.OperationResult;

public class StatusOperationResult implements OperationResult {
	private ChangeSet changeSet;

	public StatusOperationResult() {
		changeSet = new ChangeSet();
	}
	
	public void setChangeSet(ChangeSet changeSet) {
		this.changeSet = changeSet;
	}

	public ChangeSet getChangeSet() {
		return changeSet;
	}
}