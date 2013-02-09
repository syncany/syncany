package org.syncany.experimental.trash;

import java.util.TreeMap;

public class RepoDB extends RepoDBVersion {
	TreeMap<Long,RepoDBVersion> dbVersions; 
	public RepoDBVersion getNewestDBVersion() {
		return dbVersions.lastEntry().getValue();
	}
}

