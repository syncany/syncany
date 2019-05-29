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
package org.syncany.plugins.transfer.files;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.syncany.plugins.transfer.StorageException;

/**
 * The database file represents a delta database.
 *
 * <p><b>Name pattern:</b> The name pattern of a database file is
 * <b>db-&lt;clientname&gt;-&lt;clientversion&gt;</b>. Initializing an
 * instance with a non-matching name will throw an exception.
 *
 * <p><b>Note:</b> The class implements a {@link Comparable} interface and
 * can be sorted by name and client version.
 *
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
public class DatabaseRemoteFile extends RemoteFile implements Comparable<DatabaseRemoteFile> {
	private static final Pattern NAME_PATTERN = Pattern.compile("database-([^-]+)-(\\d+)");
	private static final String NAME_FORMAT = "database-%s-%010d";

	private String clientName;
	private long clientVersion;

	/**
	 * Initializes a new database file, given a name. This constructor might
	 * be called by the {@link RemoteFile#createRemoteFile(String, Class) createRemoteFile()}
	 * method of the {@link RemoteFile}.
	 *
	 * <p>If the pattern matches, the client name and the client version are set, and can be
	 * queried by {@link #getClientName()} and {@link #getClientVersion()}.
	 *
	 * @param name Database file name; <b>must</b> always match the {@link #NAME_PATTERN}
	 * @throws StorageException If the name is not match the name pattern
	 */
	public DatabaseRemoteFile(String name) throws StorageException {
		super(name);
	}

	/**
	 * Initializes a new database file, given a client name and version
	 *
	 * @param clientName The name of the client/machine for this delta database file
	 * @param version The client version for this delta database file
	 * @throws StorageException Never throws an exception
	 */
	public DatabaseRemoteFile(String clientName, long version) throws StorageException {
		super(String.format(NAME_FORMAT, clientName, version));
	}

	/**
	 * Returns the client name
	 */
	public String getClientName() {
		return clientName;
	}

	/**
	 * Returns the client version
	 */
	public long getClientVersion() {
		return clientVersion;
	}

	@Override
	protected String validateName(String name) throws StorageException {
		Matcher matcher = NAME_PATTERN.matcher(name);

		if (!matcher.matches()) {
			throw new StorageException(name + ": remote database filename pattern does not match: " + NAME_PATTERN.pattern() + " expected.");
		}

		clientName = matcher.group(1);
		clientVersion = Long.parseLong(matcher.group(2));

		return name;
	}

	@Override
	public int compareTo(DatabaseRemoteFile r2) {
		int clientNameCompare = getClientName().compareTo(r2.getClientName());

		if (clientNameCompare != 0) {
			return clientNameCompare;
		}
		else {
			return (int) (getClientVersion() - r2.getClientVersion());
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((clientName == null) ? 0 : clientName.hashCode());
		result = prime * result + (int) (clientVersion ^ (clientVersion >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof DatabaseRemoteFile)) {
			return false;
		}
		DatabaseRemoteFile other = (DatabaseRemoteFile) obj;
		if (clientName == null) {
			if (other.clientName != null) {
				return false;
			}
		}
		else if (!clientName.equals(other.clientName)) {
			return false;
		}
		if (clientVersion != other.clientVersion) {
			return false;
		}
		return true;
	}

}
