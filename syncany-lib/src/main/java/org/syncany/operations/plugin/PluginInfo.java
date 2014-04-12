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

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name="pluginInfo")
public class PluginInfo {
	@Element(name="pluginId", required=true)
	private String pluginId;
	
	@Element(name="pluginName", required=true)
	private String pluginName;

	@Element(name="pluginVersion", required=true)
	private String pluginVersion;

	@Element(name="appMinVersion", required=true)
	private String appMinVersion;

	@Element(name="sha256sum", required=true)
	private String sha256sum;

	@Element(name="downloadUrl", required=true)
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

	public String getAppMinVersion() {
		return appMinVersion;
	}

	public void setAppMinVersion(String appMinVersion) {
		this.appMinVersion = appMinVersion;
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
}