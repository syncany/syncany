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

public class RemoteDatabaseFile extends DatabaseFile {
	private static Pattern namePattern = Pattern.compile("db-([^-]+)-(\\d+)");

	public RemoteDatabaseFile(File remoteFile) {
		super(remoteFile, namePattern);
	}

	public RemoteDatabaseFile(String fileName) {
		super(fileName, namePattern);
	}

	@Override
	void initializeClientName(Matcher matcher) {
		this.clientName = matcher.group(1);
	}

	@Override
	void initializeClientVersion(Matcher matcher) {
		this.clientVersion = Long.parseLong(matcher.group(2));
	}

}
