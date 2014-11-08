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
package org.syncany.plugins.transfer;

/**
 * Represents the return structure of the tests performed by by {@link TransferManager#test(boolean)}
 * method.
 * 
 * <p>The result is determined by the following methods:
 * 
 * <ul>
 *  <li>{@link TransferManager#testTargetExists()}: Tests whether the target exists.</li>
 *  <li>{@link TransferManager#testTargetCanWrite()}: Tests whether the target is writable.</li>
 *  <li>{@link TransferManager#testTargetCanCreate()}: Tests whether the target can be created if it does not 
 *      exist already. This is only called if <tt>testCreateTarget</tt> is set.</li>
 *  <li>{@link TransferManager#testRepoFileExists()}: Tests whether the repo file exists.</li>
 * </ul>
 * 
 * @see TransferManager#test(boolean) 
 * @author Philipp Heckel <philipp.heckel@gmail.com>
 */
public class StorageTestResult {
	private boolean targetExists;
	private boolean targetCanConnect;
	private boolean targetCanCreate;
	private boolean targetCanWrite;
	private boolean repoFileExists;
	private StorageException exception;

	public boolean isTargetExists() {
		return targetExists;
	}

	public void setTargetExists(boolean targetExists) {
		this.targetExists = targetExists;
	}

	public boolean isTargetCanConnect() {
		return targetCanConnect;
	}

	public void setTargetCanConnect(boolean targetCanConnect) {
		this.targetCanConnect = targetCanConnect;
	}

	public boolean isTargetCanCreate() {
		return targetCanCreate;
	}

	public void setTargetCanCreate(boolean targetCanCreate) {
		this.targetCanCreate = targetCanCreate;
	}

	public boolean isTargetCanWrite() {
		return targetCanWrite;
	}

	public void setTargetCanWrite(boolean targetCanWrite) {
		this.targetCanWrite = targetCanWrite;
	}

	public boolean isRepoFileExists() {
		return repoFileExists;
	}

	public void setRepoFileExists(boolean repoFileExists) {
		this.repoFileExists = repoFileExists;
	}

	public StorageException getException() {
		return exception;
	}

	public void setException(StorageException exception) {
		this.exception = exception;
	}

	@Override
	public String toString() {
		return "StorageTestResult [targetExists=" + targetExists + ", targetCanConnect=" + targetCanConnect + ", targetCanCreate=" + targetCanCreate
				+ ", targetCanWrite=" + targetCanWrite + ", repoFileExists=" + repoFileExists + "]";
	}
}
