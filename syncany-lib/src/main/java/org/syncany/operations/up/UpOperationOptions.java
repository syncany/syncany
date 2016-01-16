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
package org.syncany.operations.up;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.syncany.operations.OperationOptions;
import org.syncany.operations.status.StatusOperationOptions;

@Root(name = "up")
public class UpOperationOptions implements OperationOptions {
	// The transaction size limit determines how much data (in bytes) is combined into a single transaction during an
	// up operation. Note that this is a lower bound; the Indexer and Deduper iterate through all changed files, and
	// create and commit a new DatabaseVersion whenever MultiChunks with a total size of at least this limit have been
	// processed, or when all files have been processed.
	public static final long DEFAULT_TRANSACTION_SIZE_LIMIT = 50 * 1024 * 1024;
	public static final long DEFAULT_TRANSACTION_FILE_LIMIT = 10000;

	@Element(name = "status", required = false)
	private StatusOperationOptions statusOptions = new StatusOperationOptions();

	@Element(required = false)
	private boolean forceUploadEnabled = false;

	@Element(required = false)
	private boolean resume = true;

	@Element(required = false)
	private long transactionSizeLimit = DEFAULT_TRANSACTION_SIZE_LIMIT;

	@Element(required = false)
	private long transactionFileLimit = DEFAULT_TRANSACTION_FILE_LIMIT;

	public StatusOperationOptions getStatusOptions() {
		return statusOptions;
	}

	public void setStatusOptions(StatusOperationOptions statusOptions) {
		this.statusOptions = statusOptions;
	}

	public boolean forceUploadEnabled() {
		return forceUploadEnabled;
	}

	public void setForceUploadEnabled(boolean forceUploadEnabled) {
		this.forceUploadEnabled = forceUploadEnabled;
	}

	public boolean isResume() {
		return resume;
	}

	public void setResume(boolean resume) {
		this.resume = resume;
	}

	public long getTransactionSizeLimit() {
		return transactionSizeLimit;
	}

	public void setTransactionSizeLimit(long transactionSizeLimit) {
		this.transactionSizeLimit = transactionSizeLimit;
	}

	public long getTransactionFileLimit() {
		return transactionFileLimit;
	}

	public void setTransactionFileLimit(long transactionFileLimit) {
		this.transactionFileLimit = transactionFileLimit;
	}
}