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
package org.syncany.database;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * A <tt>PartialFileHistory</tt> represents a single file in a repository over a
 * certain period of time/versions. Whenever a file is updated or deleted, a new
 * {@link FileVersion} is added to the file history.
 *
 * <p>A file history is identified by a unique random identifier and holds a sorted
 * list of file versions.
 *
 * <p>Due to cleanup mechanisms and the delta database concept, the list of file
 * versions is not always complete. The class hence represents a part of the file
 * history.
 *
 * @see FileVersion
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Fabrice Rossi
 */
public class PartialFileHistory {
	private static final byte FILE_HISTORY_ID_LENGTH = 20;

	private FileHistoryId fileHistoryId;
	private TreeMap<Long, FileVersion> versions;

	public PartialFileHistory() {
		// Required for SimpleXML.
	}

	/**
	 * Creates a new file history instance, given a {@link FileHistoryId} as identifier
	 * of the file over time. After creation, the file history's versions map is empty.
	 *
	 * @param fileHistoryId Random or non-random file history identifier
	 * @throws IllegalArgumentException If fileHistoryId is null
	 */
	public PartialFileHistory(FileHistoryId fileHistoryId) {
		if (fileHistoryId == null) {
			throw new IllegalArgumentException("Argument fileHistoryId cannot be null.");
		}

		this.fileHistoryId = fileHistoryId;
		this.versions = new TreeMap<Long, FileVersion>();
	}

	/**
	 * Returns the file history identifier for this file history. Note that
	 * this value cannot be null.
	 */
	public FileHistoryId getFileHistoryId() {
		return fileHistoryId;
	}

	/**
	 * Returns an unmodifiable map of the {@link FileVersion}s, keyed by the
	 * version number of the corresponding file version.
	 */
	public Map<Long, FileVersion> getFileVersions() {
		return Collections.unmodifiableMap(versions);
	}

	/**
	 * Returns the file version with the given file version number, or null if
	 * a version with this number does not exist in this file history.
	 */
	public FileVersion getFileVersion(long version) {
		return versions.get(version);
	}

	/**
	 * Returns the last file version in this instance of the partial file history,
	 * or <tt>null</tt> if there are no file versions.
	 *
	 * <p>Note that this method does not necessarily return the actual overall
	 * last file version, only the last of this object instance.
	 *
	 * @return Returns the last file version, or <tt>null</tt>
	 */
	public FileVersion getLastVersion() {
		if (versions.isEmpty()) {
			return null;
		}

		return versions.lastEntry().getValue();
	}

	/**
	 * Adds a new file version of the file history. The given file version is added
	 * to an internal tree map, sorted by the attribute {@link FileVersion#getVersion()}.
	 *
	 * If a file version version with the same version already exists, it is replaced by
	 * the given file version.
	 *
	 * @param fileVersion File version to be added to the file history
	 * @throws IllegalArgumentException If fileVersion or its version number is <tt>null</tt>
	 */
	public void addFileVersion(FileVersion fileVersion) {
		if (fileVersion == null || fileVersion.getVersion() == null) {
			throw new IllegalArgumentException("Argument fileVersion or fileVersion.getVersion() cannot be null.");
		}

		versions.put(fileVersion.getVersion(), fileVersion);
	}

	/**
	 * Clones the file history, including its file versions. Note that file versions
	 * are not cloned, but copied by reference.
	 *
	 * @return Returns cloned file history
	 */
	@Override
	public PartialFileHistory clone() {
		PartialFileHistory clone = new PartialFileHistory(fileHistoryId);
		clone.versions.putAll(versions);

		return clone;
	}

	@Override
	public String toString() {
		return PartialFileHistory.class.getSimpleName() + "(fileId=" + fileHistoryId + ", versions=" + versions + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileHistoryId == null) ? 0 : fileHistoryId.hashCode());
		result = prime * result + ((versions == null) ? 0 : versions.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof PartialFileHistory)) {
			return false;
		}
		PartialFileHistory other = (PartialFileHistory) obj;
		if (fileHistoryId == null) {
			if (other.fileHistoryId != null) {
				return false;
			}
		}
		else if (!fileHistoryId.equals(other.fileHistoryId)) {
			return false;
		}
		if (versions == null) {
			if (other.versions != null) {
				return false;
			}
		}
		else if (!versions.equals(other.versions)) {
			return false;
		}
		return true;
	}

	/**
	 * The file history identifier (also: file identifier) is a key to identify a single file
	 * throughout its lifetime. In particular, it does not only identify
	 *
	 */
	public static class FileHistoryId extends ObjectId {
		private FileHistoryId(byte[] array) {
			super(array);
		}

		public static FileHistoryId secureRandomFileId() {
			return new FileHistoryId(ObjectId.secureRandomBytes(FILE_HISTORY_ID_LENGTH));
		}

		public static FileHistoryId parseFileId(String s) {
			return new FileHistoryId(ObjectId.parseObjectId(s));
		}
	}
}
