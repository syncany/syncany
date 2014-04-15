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

import java.util.List;

import org.syncany.operations.OperationResult;

public class PluginOperationResult implements OperationResult {
	public enum PluginResultCode {
		OK, NOK
	}
	
	private PluginResultCode resultCode;
	private List<ExtendedPluginInfo> pluginList;
	private String sourcePluginPath;
	private String targetPluginPath;
	private PluginInfo affectedPluginInfo;

	public List<ExtendedPluginInfo> getPluginList() {
		return pluginList;
	}

	public void setPluginList(List<ExtendedPluginInfo> pluginList) {
		this.pluginList = pluginList;
	}

	public PluginResultCode getResultCode() {
		return resultCode;
	}

	public void setResultCode(PluginResultCode resultCode) {
		this.resultCode = resultCode;
	}

	public PluginInfo getAffectedPluginInfo() {
		return affectedPluginInfo;
	}

	public void setAffectedPluginInfo(PluginInfo affectedPluginInfo) {
		this.affectedPluginInfo = affectedPluginInfo;
	}

	public String getSourcePluginPath() {
		return sourcePluginPath;
	}

	public void setSourcePluginPath(String affectedPluginPath) {
		this.sourcePluginPath = affectedPluginPath;
	}

	public String getTargetPluginPath() {
		return targetPluginPath;
	}

	public void setTargetPluginPath(String targetPluginPath) {
		this.targetPluginPath = targetPluginPath;
	}
}
