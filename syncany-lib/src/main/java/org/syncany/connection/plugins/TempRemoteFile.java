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

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The temp file represents a temporary file on the remote storage. 
 * 
 * <p><b>Name pattern:</b> The name pattern of a temp file is
 * <b>temp-&lt;filehexhashcode&gt;</b>.
 * 
 * @author Pim Otte
 */
public class TempRemoteFile extends RemoteFile {
	private static final Pattern NAME_PATTERN = Pattern.compile("temp-([a-f0-9]+)");
	private static final String NAME_FORMAT = "temp-%s";

	/**
	 * Initializes a new temp file, given a name. 
	 * 
	 * @param name temp file name; <b>must</b> always match the {@link #NAME_PATTERN} 
	 * @throws StorageException If the name is not match the name pattern
	 */
	public TempRemoteFile(String name) throws StorageException {
		super(name);
	}
	
	/**
	 * Initializes a new temp file, given the local file which will be moved there. 
	 * 
	 * @param localFile the local file that will be moved to this temporary location.
	 * @throws StorageException If the name is not match the name pattern
	 */
	public TempRemoteFile(File localFile) throws StorageException {
		super(String.format(NAME_FORMAT, Integer.toHexString(localFile.hashCode())));
	}

	@Override
	protected String validateName(String name) throws StorageException {
		Matcher matcher = NAME_PATTERN.matcher(name);

		if (!matcher.matches()) {
			throw new StorageException(name + ": remote filename pattern does not match: " + NAME_PATTERN.pattern() + " expected.");
		}

		return name;
	}
}
