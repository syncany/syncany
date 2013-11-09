/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.database;

import java.util.Set;
import java.util.TreeMap;

public class Branches {
	private TreeMap<String, Branch> branches;
	
	public Branches() {
		this.branches = new TreeMap<String, Branch>();
	}
	
	public Set<String> getClients() {
		return branches.keySet();
	}
	
	public Branch getBranch(String client) {
		return getBranch(client, false);
	}
	
	public Branch getBranch(String client, boolean createIfNotExistant) {
		Branch branch = branches.get(client);
		
		if (branch == null && createIfNotExistant) {
			branch = new Branch();
			branches.put(client, branch);
		}
		
		return branch;
	}

	public void put(String machineName, Branch branch) {
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
	public Branches clone() {
		Branches clonedBranches = new Branches();
		clonedBranches.branches.putAll(branches);
		
		return clonedBranches;
	}
}
