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
package org.syncany.daemon.websocket.messages;

import java.util.Map;

/**
 * @author vincent
 *
 */
public class DaemonAbstractInitMessage extends DaemonMessage {
	
	private Map<String, String> pluginArgs;
	private String pluginId;
	private String password;
	
	public Map<String, String> getPluginArgs() {
		return pluginArgs;
	}
	public void setPluginArgs(Map<String, String> pluginArgs) {
		this.pluginArgs = pluginArgs;
	}
	
	public DaemonAbstractInitMessage(DaemonMessage parent) {
		super(parent);
	}

	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getPluginId() {
		return pluginId;
	}
	public void setPluginId(String pluginId) {
		this.pluginId = pluginId;
	}
}
