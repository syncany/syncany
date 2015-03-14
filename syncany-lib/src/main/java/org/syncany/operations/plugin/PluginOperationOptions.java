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
package org.syncany.operations.plugin;

import org.syncany.operations.OperationOptions;

public class PluginOperationOptions implements OperationOptions {
	public enum PluginListMode {
		ALL, LOCAL, REMOTE
	}

	private PluginOperationAction action = null;
	private String pluginId = null;
	private PluginListMode listMode = PluginListMode.ALL;
	private boolean snapshots = false;
	private String apiEndpoint = null;

	public PluginOperationAction getAction() {
		return action;
	}

	public void setAction(PluginOperationAction action) {
		this.action = action;
	}

	public String getPluginId() {
		return pluginId;
	}

	public void setPluginId(String pluginId) {
		this.pluginId = pluginId;
	}

	public PluginListMode getListMode() {
		return listMode;
	}

	public void setListMode(PluginListMode listMode) {
		this.listMode = listMode;
	}

	public boolean isSnapshots() {
		return snapshots;
	}

	public void setSnapshots(boolean snapshots) {
		this.snapshots = snapshots;
	}

	public String getApiEndpoint() {
		return apiEndpoint;
	}

	public void setApiEndpoint(String apiEndpoint) {
		this.apiEndpoint = apiEndpoint;
	}
}