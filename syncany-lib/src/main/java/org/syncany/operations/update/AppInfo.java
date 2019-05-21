/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

/**
 * This class represents an information object describing an application release 
 * for a single distribution type. It is returned by the Syncany API during the
 * {@link UpdateOperation} with the action {@link UpdateOperationAction#CHECK}. 
 *  
 * <p>this class uses the Simple framework for XML serialization, and its corresponding
 * annotation-based configuration.  
 *  
 * @see <a href="http://simple.sourceforge.net/">Simple framework</a>
 * @see <a href="https://github.com/syncany/syncany-website">Syncany Website/API</a>
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
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

	public String getType() {
		return type;
	}

	public String getAppVersion() {
		return appVersion;
	}

	public String getDate() {
		return date;
	}

	public boolean isRelease() {
		return release;
	}

	public String getOperatingSystem() {
		return operatingSystem;
	}

	public String getArchitecture() {
		return architecture;
	}

	public boolean isChecksum() {
		return checksum;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}
}