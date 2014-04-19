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
package org.syncany.connection.plugins;

/**
 * @author pheckel
 *
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
