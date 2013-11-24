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

import org.syncany.util.StringUtil;

public class MultiChunkRemoteFile extends RemoteFile {
	private static final Pattern NAME_PATTERN = Pattern.compile("multichunk-([a-f0-9]+)");
	private static final String NAME_FORMAT = "multichunk-%s";
	
	private byte[] multiChunkId;
	
	public MultiChunkRemoteFile(String name) throws StorageException {
		super(name);
	}
		
	public MultiChunkRemoteFile(byte[] multiChunkId) throws StorageException {
		super(String.format(NAME_FORMAT, StringUtil.toHex(multiChunkId))); 
	}
		
	public byte[] getMultiChunkId() {
		return multiChunkId;
	}
	
	@Override
	protected String parseName(String name) throws StorageException {
		Matcher matcher = NAME_PATTERN.matcher(name);
		
		if (!matcher.matches()) {
			throw new StorageException(name + ": remote filename pattern does not match: " + NAME_PATTERN.pattern() + " expected.");
		}
		
		try {
			multiChunkId = StringUtil.fromHex(matcher.group(1));
		}
		catch (Exception e) {
			throw new StorageException(e);
		}
		
		return name;
	}
}
