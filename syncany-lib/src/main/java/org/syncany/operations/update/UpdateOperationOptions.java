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

import org.syncany.operations.OperationOptions;

/**
 * Options class to configure the {@link UpdateOperation}.
 * The options alter/influence the behavior of the operation.
 * 
 * @see <a href="https://github.com/syncany/syncany-website">Syncany Website/API</a>
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class UpdateOperationOptions implements OperationOptions {
	private UpdateOperationAction action = null;
	private boolean snapshots = false;
	private String apiEndpoint = null;

	/**
	 * Set the action to execute when the {@link UpdateOperation} 
	 * is run. Depending on the action, the bavior of the operation
	 * is changed/altered.
	 */
	public void setAction(UpdateOperationAction action) {
		this.action = action;
	}

	/**
	 * Set whether or not snapshots are included when the Syncany
	 * API is queried for updates. 
	 */
	public void setSnapshots(boolean snapshots) {
		this.snapshots = snapshots;
	}
	
	/**
	 * Set an alternative API endpoint. If left as set by default,
	 * the default API endpoint will be used.
	 */
	public void setApiEndpoint(String apiEndpoint) {
		this.apiEndpoint = apiEndpoint;
	}

	/**
	 * Get the update action 
	 */
	public UpdateOperationAction getAction() {
		return action;
	}

	/**
	 * Get whether or not snapshots are included
	 * when the Syncany API is queried. 
	 */
	public boolean isSnapshots() {
		return snapshots;
	}

	/**
	 * Return the API endpoint (if changed), or null
	 * otherwise.
	 */
	public String getApiEndpoint() {
		return apiEndpoint;
	}
}