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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.util.StringUtil;

/**
 * The multichunk file represents a multichunk on the remote storage. 
 * 
 * <p><b>Name pattern:</b> The name pattern of a multichunk file is
 * <b>multichunk-&lt;multichunkid&gt;</b>. Initializing an 
 * instance with a non-matching name will throw an exception.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class MultiChunkRemoteFile extends RemoteFile {
	private static final Pattern NAME_PATTERN = Pattern.compile("multichunk-([a-f0-9]+)");
	private static final String NAME_FORMAT = "multichunk-%s";

	private byte[] multiChunkId;

	/**
	 * Initializes a new multichunk file, given a name. This constructor might 
	 * be called by the {@link RemoteFileFactory#createRemoteFile(String, Class) createRemoteFile()}
	 * method of the {@link RemoteFileFactory}. 
	 * 
	 * <p>If the pattern matches, the multichunk identifier is set and can be  
	 * queried by {@link #getMultiChunkId()}.
	 *  
	 * @param name Multichunk file name; <b>must</b> always match the {@link #NAME_PATTERN} 
	 * @throws StorageException If the name is not match the name pattern
	 */
	public MultiChunkRemoteFile(String name) throws StorageException {
		super(name);
	}

	/**
	 * Initializes a new multichunk file, given a multichunk identifier
	 *  
	 * @param multiChunkId The identifier of the multichunk
	 * @throws StorageException Never throws an exception
	 */
	public MultiChunkRemoteFile(MultiChunkId multiChunkId) throws StorageException {
		super(String.format(NAME_FORMAT, multiChunkId.toString()));
	}

	/**
	 * Returns the multichunk identifier
	 */
	public byte[] getMultiChunkId() {
		return multiChunkId;
	}

	@Override
	protected String validateName(String name) throws StorageException {
		Matcher matcher = NAME_PATTERN.matcher(name);

		if (!matcher.matches()) {
			throw new StorageException(name + ": remote filename pattern does not match: " + NAME_PATTERN.pattern() + " expected.");
		}

		try {
			multiChunkId = StringUtil.fromHex(matcher.group(1));
		}
		catch (Exception e) {
			throw new StorageException(name + ": remote filename pattern does not match (invalid hex): " + NAME_PATTERN.pattern() + " expected.");
		}

		return name;
	}
}
