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
public class DeamonWatchMessage extends DaemonMessage {

	public DeamonWatchMessage(DaemonMessage parent) {
		super(parent);
		setAction("watch");
	}
	
	private int interval;
	boolean automaticWatcher;
	
	public int getInterval() {
		return interval;
	}
	public void setInterval(int interval) {
		this.interval = interval;
	}
	
	public boolean isAutomaticWatcher() {
		return automaticWatcher;
	}
	public void setAutomaticWatcher(boolean automaticWatcher) {
		this.automaticWatcher = automaticWatcher;
	}
}
