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

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract public class DatabaseFile implements Comparable<DatabaseFile>{
	protected String clientName;
	protected long clientVersion;
	private String fileName;
	private File file;
	
	public DatabaseFile(File file, Pattern namePattern) {
		this.file = file;
		this.fileName = file.getName();

		validateDatabaseFileName(namePattern);
	}
	
	public DatabaseFile(String fileName, Pattern namePattern) {
		this.fileName = fileName;
		validateDatabaseFileName(namePattern);
	}

	private void validateDatabaseFileName(Pattern namePattern) {
		Matcher matcher = namePattern.matcher(this.fileName);
		
		if (!matcher.matches()) {
			throw new RuntimeException(this.fileName + " - Remote database filename pattern does not match: " + namePattern.pattern() + " expected.");
		}
		
		initializeClientName(matcher);
		initializeClientVersion(matcher);
	}
		
	abstract void initializeClientName(Matcher matcher);
	abstract void initializeClientVersion(Matcher matcher);

	public String getClientName() {
		return clientName;
	}

	public long getClientVersion() {
		return clientVersion;
	}	
	
	public File getFile() {
		return this.file;
	}
	
	@Override
	public int compareTo(DatabaseFile r2) {
		int clientNameCompare = this.getClientName().compareTo(r2.getClientName());

		if (clientNameCompare != 0) {
			return clientNameCompare;
		} else {
			return (int) (this.getClientVersion() - r2.getClientVersion());
		}
	}
	
}
