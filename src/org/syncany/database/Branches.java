package org.syncany.database;

import java.util.Map;
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

	public void add(String machineName, Branch branch) {
		branches.put(machineName, branch);		
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
