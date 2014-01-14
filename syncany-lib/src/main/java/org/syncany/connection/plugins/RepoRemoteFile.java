/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

/**
 * The repo file represents the repository-defining file. It is used to
 * describe the chunking and encryption parameters of an an initialized
 * repository.
 * 
 * <p><b>Name pattern:</b> The file must always be called <b>syncany</b>
 * Initializing an instance with a different name will throw an
 * exception.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class RepoRemoteFile extends RemoteFile {
	private static final String NAME_FORMAT = "syncany";
	
	/**
	 * Initializes a new repo file with the name <b>syncany</b>.
	 * @throws StorageException Never throws an exception.
	 */
	public RepoRemoteFile() throws StorageException {
		super(NAME_FORMAT);
	}	
	
	/**
	 * Initializes a new repo file, given a name. This constructor might 
	 * be called by the {@link RemoteFileFactory#createRemoteFile(String, Class) createRemoteFile()}
	 * method of the {@link RemoteFileFactory}. 
	 *  
	 * @param name Repo file name; <b>must</b> always be <b>syncany</b> 
	 * @throws StorageException If the name is not <b>syncany</b>
	 */
	public RepoRemoteFile(String name) throws StorageException {
		super(name);
	}

	@Override
	protected String validateName(String name) throws StorageException {
		if (!NAME_FORMAT.equals(name)) {
			throw new StorageException(name + ": remote filename pattern does not match: " + NAME_FORMAT + " expected.");
		}
		
		return name;
	}
}
