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
package org.syncany.operations.down;

import java.util.Set;
import java.util.TreeMap;

/**
 * The database branches class is a convenience class to bundle multiple
 * {@link DatabaseBranch}s into one object, and map it to its corresponding
 * owner machine name.
 * 
 * <p>The class is mainly used by the {@link DatabaseReconciliator} when comparing
 * and reconciling changes between the clients' database branches. 
 *    
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
public class DatabaseBranches {
	private TreeMap<String, DatabaseBranch> branches;
	
	public DatabaseBranches() {
		this.branches = new TreeMap<String, DatabaseBranch>();
	}
	
	public Set<String> getClients() {
		return branches.keySet();
	}
	
	public DatabaseBranch getBranch(String client) {
		return getBranch(client, false);
	}
	
	public DatabaseBranch getBranch(String client, boolean createIfNotExistant) {
		DatabaseBranch branch = branches.get(client);
		
		if (branch == null && createIfNotExistant) {
			branch = new DatabaseBranch();
			branches.put(client, branch);
		}
		
		return branch;
	}

	public void put(String machineName, DatabaseBranch branch) {
		branches.put(machineName, branch);		
	}	

	public void remove(String machineName) {
		branches.remove(machineName);
	}	
	
	@Override
	public String toString() {
		return branches.toString();
	}
	
	@Override
	public DatabaseBranches clone() {
		DatabaseBranches clonedBranches = new DatabaseBranches();
		clonedBranches.branches.putAll(branches);
		
		return clonedBranches;
	}
}
