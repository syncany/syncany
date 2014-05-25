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
package org.syncany.operations.up;

import org.syncany.operations.ChangeSet;
import org.syncany.operations.OperationResult;
import org.syncany.operations.cleanup.CleanupOperationResult;
import org.syncany.operations.status.StatusOperationResult;

public class UpOperationResult implements OperationResult {
	public enum UpResultCode {
		OK_CHANGES_UPLOADED, OK_NO_CHANGES, NOK_UNKNOWN_DATABASES
	};

	private UpResultCode resultCode;
	private StatusOperationResult statusResult = new StatusOperationResult();
	private CleanupOperationResult cleanupResult = null;
	private ChangeSet uploadChangeSet = new ChangeSet();

	public CleanupOperationResult getCleanupResult() {
		return cleanupResult;
	}

	public void setCleanupResult(CleanupOperationResult cleanupResult) {
		this.cleanupResult = cleanupResult;
	}

	public UpResultCode getResultCode() {
		return resultCode;
	}

	public void setResultCode(UpResultCode resultCode) {
		this.resultCode = resultCode;
	}

	public void setStatusResult(StatusOperationResult statusResult) {
		this.statusResult = statusResult;
	}

	public void setUploadChangeSet(ChangeSet uploadChangeSet) {
		this.uploadChangeSet = uploadChangeSet;
	}

	public StatusOperationResult getStatusResult() {
		return statusResult;
	}

	public ChangeSet getChangeSet() {
		return uploadChangeSet;
	}
}