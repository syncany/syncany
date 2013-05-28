package org.syncany.operations;

import org.syncany.database.DatabaseVersion;

public class DatabaseVersionPair {
	private DatabaseVersion firstDatabaseVersion;
	private DatabaseVersion secondDatabaseVersion;		

	public DatabaseVersionPair(DatabaseVersion firstDatabaseVersion, DatabaseVersion secondDatabaseVersion) {
		this.firstDatabaseVersion = firstDatabaseVersion;
		this.secondDatabaseVersion = secondDatabaseVersion;
	}

	public DatabaseVersion getFirstDatabaseVersion() {
		return firstDatabaseVersion;
	}

	public DatabaseVersion getSecondDatabaseVersion() {
		return secondDatabaseVersion;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((firstDatabaseVersion == null) ? 0 : firstDatabaseVersion.hashCode());
		result = prime * result + ((secondDatabaseVersion == null) ? 0 : secondDatabaseVersion.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DatabaseVersionPair other = (DatabaseVersionPair) obj;
		if (firstDatabaseVersion == null) {
			if (other.firstDatabaseVersion != null)
				return false;
		} else if (!firstDatabaseVersion.equals(other.firstDatabaseVersion))
			return false;
		if (secondDatabaseVersion == null) {
			if (other.secondDatabaseVersion != null)
				return false;
		} else if (!secondDatabaseVersion.equals(other.secondDatabaseVersion))
			return false;
		return true;
	}
}	