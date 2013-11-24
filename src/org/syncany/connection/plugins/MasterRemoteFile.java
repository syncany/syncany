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

public class MasterRemoteFile extends RemoteFile {
	private static final String NAME_FORMAT = "master";

	public MasterRemoteFile() throws StorageException {
		super(NAME_FORMAT);
	}	
	
	public MasterRemoteFile(String name) throws StorageException {
		super(name);
	}

	@Override
	protected String parseName(String name) throws StorageException {
		if (!NAME_FORMAT.equals(name)) {
			throw new StorageException(name + ": remote filename pattern does not match: " + NAME_FORMAT + " expected.");
		}
		
		return name;
	}
}
