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

import java.util.ArrayList;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * This class represents the response from the Syncany API when checking 
 * the newest application versions. The response object returns a list 
 * of {@link AppInfo} objects, one for each distribution type and operating
 * system.
 * 
 * <p>this class uses the Simple framework for XML serialization, and its corresponding
 * annotation-based configuration.  
 *  
 * @see <a href="http://simple.sourceforge.net/">Simple framework</a>
 * @see <a href="https://github.com/syncany/syncany-website">Syncany Website/API</a>
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
@Root(name = "appInfoResponse")
public class AppInfoResponse {
	@Element(name = "code", required = true)
	private int code;

	@Element(name = "message", required = true)
	private String message;

	@ElementList(name = "appInfoList", required = true)
	private ArrayList<AppInfo> appInfoList;

	public AppInfoResponse() {
		// Required default constructor
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public ArrayList<AppInfo> getAppInfoList() {
		return appInfoList;
	}

	public void setAppInfoList(ArrayList<AppInfo> appInfoList) {
		this.appInfoList = appInfoList;
	}
}