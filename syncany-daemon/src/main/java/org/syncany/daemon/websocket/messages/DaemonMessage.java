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


/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class DaemonMessage {
	private long timeStamp;
	private String commandId;
	private String action;
	private String clientId;
	private String clientType;
	private String localFolder;
	private String daemonIdentifier;
	
	public DaemonMessage() {
		this.timeStamp = System.nanoTime();
	}
	
	public DaemonMessage(DaemonMessage parent) {
		this.timeStamp = parent.getTimeStamp();
		this.action = parent.getAction();
		this.clientId = parent.getClientId();
		this.clientType = parent.getClientType();
		this.localFolder = parent.getLocalFolder();
		this.commandId = parent.getCommandId();
		this.daemonIdentifier = parent.getDaemonIdentifier();
	}
	
	public String getCommandId() {
		return commandId;
	}
	public void setCommandId(String commandId) {
		this.commandId = commandId;
	}

	public long getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	
	public String getClientType() {
		return clientType;
	}
	public void setClientType(String clientType) {
		this.clientType = clientType;
	}
	
	public String getLocalFolder() {
		return localFolder;
	}
	public void setLocalFolder(String localFolder) {
		this.localFolder = localFolder;
	}
	
	public void setDaemonIdentifier(String daemonIdentifier) {
		this.daemonIdentifier = daemonIdentifier;
	}
	public String getDaemonIdentifier() {
		return daemonIdentifier;
	}
}
