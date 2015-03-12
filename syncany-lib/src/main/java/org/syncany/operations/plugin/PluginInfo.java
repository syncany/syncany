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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.syncany.util.StringUtil;

@Root(name = "pluginInfo", strict = false)
public class PluginInfo {
	@Element(name = "pluginId", required = true)
	private String pluginId;

	@Element(name = "pluginName", required = false)
	private String pluginName;

	@Element(name = "pluginVersion", required = false)
	private String pluginVersion;
	
	@Element(name = "pluginOperatingSystem", required = false)
	private String pluginOperatingSystem;
	
	@Element(name = "pluginArchitecture", required = false)
	private String pluginArchitecture;	

	@Element(name = "pluginDate", required = false)
	private String pluginDate;

	@Element(name = "pluginAppMinVersion", required = false)
	private String pluginAppMinVersion;

	@Element(name = "pluginRelease", required = false)
	private boolean pluginRelease;

	@Element(name = "pluginConflictsWith", required = false)
	private String conflictingPluginIds; // comma-separated

	@Element(name = "pluginThirdParty", required = false)
	private boolean pluginThirdParty; 

	@Element(name = "sha256sum", required = false)
	private String sha256sum;

	@Element(name = "downloadUrl", required = false)
	private String downloadUrl;

	public PluginInfo() {
		// Nothing.
	}

	public String getPluginId() {
		return pluginId;
	}

	public void setPluginId(String pluginId) {
		this.pluginId = pluginId;
	}

	public String getPluginName() {
		return pluginName;
	}

	public void setPluginName(String pluginName) {
		this.pluginName = pluginName;
	}

	public String getPluginVersion() {
		return pluginVersion;
	}

	public void setPluginVersion(String pluginVersion) {
		this.pluginVersion = pluginVersion;
	}

	public String getPluginOperatingSystem() {
		return pluginOperatingSystem;
	}

	public void setPluginOperatingSystem(String pluginOperatingSystem) {
		this.pluginOperatingSystem = pluginOperatingSystem;
	}

	public String getPluginArchitecture() {
		return pluginArchitecture;
	}

	public void setPluginArchitecture(String pluginArchitecture) {
		this.pluginArchitecture = pluginArchitecture;
	}

	public String getPluginDate() {
		return pluginDate;
	}

	public void setPluginDate(String pluginDate) {
		this.pluginDate = pluginDate;
	}

	public String getPluginAppMinVersion() {
		return pluginAppMinVersion;
	}

	public void setPluginAppMinVersion(String pluginAppMinVersion) {
		this.pluginAppMinVersion = pluginAppMinVersion;
	}

	public boolean isPluginRelease() {
		return pluginRelease;
	}

	public void setPluginRelease(boolean pluginRelease) {
		this.pluginRelease = pluginRelease;
	}

	public String getSha256sum() {
		return sha256sum;
	}

	public void setSha256sum(String sha256sum) {
		this.sha256sum = sha256sum;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	public boolean isPluginThirdParty() {
		return pluginThirdParty;
	}

	public void setPluginThirdParty(boolean pluginThirdParty) {
		this.pluginThirdParty = pluginThirdParty;
	}

	public List<String> getConflictingPluginIds() {
		if (conflictingPluginIds != null) {
			return Arrays.asList(conflictingPluginIds.split(","));
		}
		else {
			return new ArrayList<String>();
		}
	}

	public void setConflictingPluginIds(List<String> conflictingPluginIds) {
		this.conflictingPluginIds = (conflictingPluginIds != null) ? StringUtil.join(conflictingPluginIds, ",") : null;
	}
}