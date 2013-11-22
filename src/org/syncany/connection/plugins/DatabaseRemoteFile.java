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
package org.syncany.connection.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseRemoteFile extends RemoteFile implements Comparable<DatabaseRemoteFile> {
	private static final Pattern NAME_PATTERN = Pattern.compile("db-([^-]+)-(\\d+)");
	private static final String NAME_FORMAT = "db-%s-%010d";
	
	private String clientName;
	private long clientVersion;
	
	public DatabaseRemoteFile(String name) {
		super(name);
		parseName();
	}
		
	public DatabaseRemoteFile(String clientName, long version) {
		super(String.format(NAME_FORMAT, clientName, version)); 
	}
		
	public String getClientName() {
		return clientName;
	}
	
	public long getClientVersion() {
		return clientVersion;
	}
	
	private void parseName() {
		Matcher matcher = NAME_PATTERN.matcher(getName());
		
		if (!matcher.matches()) {
			throw new RuntimeException(getName() + ": remote database filename pattern does not match: " + NAME_PATTERN.pattern() + " expected.");
		}
		
		clientName = matcher.group(1);
		clientVersion = Long.parseLong(matcher.group(2));
	}


	@Override
	public int compareTo(DatabaseRemoteFile r2) {
		int clientNameCompare = this.getClientName().compareTo(r2.getClientName());

		if (clientNameCompare != 0) {
			return clientNameCompare;
		}
		else {
			return (int) (this.getClientVersion() - r2.getClientVersion());
		}
	}
}
