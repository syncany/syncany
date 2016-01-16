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
package org.syncany.operations.daemon;

import java.util.ArrayList;
import java.util.List;

import org.syncany.operations.OperationOptions;

public class DaemonOperationOptions implements OperationOptions {
	public enum DaemonAction {
		RUN, LIST, ADD, REMOVE
	}
	
	private DaemonAction action;
	private List<String> watchRoots;
	
	public DaemonOperationOptions() {
		this.action = null;
		this.watchRoots = new ArrayList<>();
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
	
	public List<String> getWatchRoots() {
		return watchRoots;
	}
	
	public void setWatchRoots(List<String> watchRoots) {
		this.watchRoots = watchRoots;
	}
}