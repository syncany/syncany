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
package org.syncany.operations.update;

import org.syncany.operations.OperationResult;

public class UpdateOperationResult implements OperationResult {
	public enum UpdateResultCode {
		OK, NOK
	}

	private UpdateResultCode resultCode;
	private UpdateOperationAction action;
	
	private AppInfo appInfo;
	private boolean newVersionAvailable;

	public UpdateOperationAction getAction() {
		return action;
	}

	public void setAction(UpdateOperationAction action) {
		this.action = action;
	}

	public UpdateResultCode getResultCode() {
		return resultCode;
	}

	public void setResultCode(UpdateResultCode resultCode) {
		this.resultCode = resultCode;
	}

	public AppInfo getAppInfo() {
		return appInfo;
	}

	public void setAppInfo(AppInfo appInfo) {
		this.appInfo = appInfo;
	}

	public boolean isNewVersionAvailable() {
		return newVersionAvailable;
	}

	public void setNewVersionAvailable(boolean newVersionAvailable) {
		this.newVersionAvailable = newVersionAvailable;
	}
}
