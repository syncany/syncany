package org.syncany.experimental.trash;

import java.util.TreeMap;
import java.util.Collection;
import java.util.Collections;

public class RepositoryDatabase extends RepositoryDatabaseVersion {
	private TreeMap<Long, RepositoryDatabaseVersion> dbVersions;
	
	public RepositoryDatabase() {
		dbVersions = new TreeMap<Long, RepositoryDatabaseVersion>();
	}

	public RepositoryDatabaseVersion getNewestDatabaseVersion() {
		return dbVersions.lastEntry().getValue();
	}
	
	public Collection<RepositoryDatabaseVersion> getRepositoryDatabaseVersions() {
		return Collections.unmodifiableCollection(dbVersions.values());
	}
	
	public void addRepositoryDatabaseVersion(RepositoryDatabaseVersion newVersion) {
		long newVersionKey = dbVersions.size() > 0 ? dbVersions.lastKey() + 1 : 1;
		newVersion.setCurrentVersion(newVersionKey);
		dbVersions.put(newVersionKey, newVersion);
	}
}

