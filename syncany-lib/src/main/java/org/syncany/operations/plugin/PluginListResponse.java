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

import java.util.ArrayList;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

@Root(name="pluginListResponse")
@Namespace(reference="http://syncany.org/plugins/1/list")
public class PluginListResponse {
	@Element(name = "code", required = true)
	private int code;

	@Element(name = "message", required = true)
	private String message;

	@ElementList(name = "plugins", required = true)
	private ArrayList<PluginInfo> plugins;

	public PluginListResponse() {
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

	public ArrayList<PluginInfo> getPlugins() {
		return plugins;
	}

	public void setPlugins(ArrayList<PluginInfo> plugins) {
		this.plugins = plugins;
	}
}