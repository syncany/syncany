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

import org.syncany.plugins.transfer.StorageException;

/**
 * The master file represents the file that stores the salt for the master
 * key. The file is only mandatory if the repository is encrypted. 
 * 
 * <p><b>Name pattern:</b> The file must always be called <b>master</b>
 * Initializing an instance with a different name will throw an
 * exception.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class MasterRemoteFile extends RemoteFile {
	private static final String NAME_FORMAT = "master";

	/**
	 * Initializes a new master file with the name <b>master</b>.
	 * @throws StorageException Never throws an exception.
	 */
	public MasterRemoteFile() throws StorageException {
		super(NAME_FORMAT);
	}	
	
	/**
	 * Initializes a new master file, given a name. This constructor might 
	 * be called by the {@link RemoteFileFactory#createRemoteFile(String, Class) createRemoteFile()}
	 * method of the {@link RemoteFileFactory}. 
	 *  
	 * @param name Master file name; <b>must</b> always be <b>master</b> 
	 * @throws StorageException If the name is not <b>master</b>
	 */
	public MasterRemoteFile(String name) throws StorageException {
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
