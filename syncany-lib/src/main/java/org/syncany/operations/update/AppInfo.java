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

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "appInfo", strict = false)
public class AppInfo {
	@Element(name = "dist", required = true)
	private String dist;

	@Element(name = "type", required = true)
	private String type;

	@Element(name = "appVersion", required = true)
	private String appVersion;
	
	@Element(name = "date", required = true)
	private String date;
	
	@Element(name = "release", required = true)
	private boolean release;	

	@Element(name = "operatingSystem", required = true)
	private String operatingSystem;

	@Element(name = "architecture", required = true)
	private String architecture;

	@Element(name = "checksum", required = true)
	private boolean checksum;

	@Element(name = "downloadUrl", required = true)
	private String downloadUrl;

	public AppInfo() {
		// Nothing.
	}

	public String getDist() {
		return dist;
	}

	public void setDist(String dist) {
		this.dist = dist;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAppVersion() {
		return appVersion;
	}

	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public boolean isRelease() {
		return release;
	}

	public void setRelease(boolean release) {
		this.release = release;
	}

	public String getOperatingSystem() {
		return operatingSystem;
	}

	public void setOperatingSystem(String operatingSystem) {
		this.operatingSystem = operatingSystem;
	}

	public String getArchitecture() {
		return architecture;
	}

	public void setArchitecture(String architecture) {
		this.architecture = architecture;
	}

	public boolean isChecksum() {
		return checksum;
	}

	public void setChecksum(boolean checksum) {
		this.checksum = checksum;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}
}