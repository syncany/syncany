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
package org.syncany.operations.daemon;

import org.syncany.operations.OperationOptions;

public class DaemonOperationOptions implements OperationOptions {
	public enum DaemonAction {
		RUN, LIST, ADD, REMOVE
	}
	
	private DaemonAction action = null;
	private String watchRoot = null;
	
	public DaemonOperationOptions() {
		// Nothing
	}
	
	public DaemonOperationOptions(DaemonAction action) {
		this.action = action;
	}
	
	public DaemonAction getAction() {
		return action;
	}
	
	public void setAction(DaemonAction action) {
		this.action = action;
	}
	
	public String getWatchRoot() {
		return watchRoot;
	}
	
	public void setWatchRoot(String watchRoot) {
		this.watchRoot = watchRoot;
	}
}