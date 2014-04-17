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
package org.syncany.operations.plugin;

public class ExtendedPluginInfo {
	private PluginInfo localPluginInfo;
	private PluginInfo remotePluginInfo;
	private boolean installed;
	private boolean remoteAvailable;
	private boolean upgradeAvailable;
	
	public PluginInfo getLocalPluginInfo() {
		return localPluginInfo;
	}
	
	public void setLocalPluginInfo(PluginInfo localPluginInfo) {
		this.localPluginInfo = localPluginInfo;
	}
	
	public PluginInfo getRemotePluginInfo() {
		return remotePluginInfo;
	}
	
	public void setRemotePluginInfo(PluginInfo remotePluginInfo) {
		this.remotePluginInfo = remotePluginInfo;
	}
	
	public boolean isInstalled() {
		return installed;
	}
	
	public void setInstalled(boolean installed) {
		this.installed = installed;
	}
	
	public boolean isRemoteAvailable() {
		return remoteAvailable;
	}

	public void setRemoteAvailable(boolean remoteAvailable) {
		this.remoteAvailable = remoteAvailable;
	}

	public boolean isUpgradeAvailable() {
		return upgradeAvailable;
	}
	
	public void setUpgradeAvailable(boolean upgradeAvailable) {
		this.upgradeAvailable = upgradeAvailable;
	}
}