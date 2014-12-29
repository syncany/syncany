/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import org.syncany.crypto.CipherUtil;
import org.syncany.plugins.transfer.StorageException;

/**
 * The temp file represents a temporary file on the remote storage. 
 * 
 * <p><b>Name pattern:</b> The name pattern of a temp file is
 * <b>temp-&lt;randomidentifier&gt;</b>.
 * 
 * @author Pim Otte
 */
public class TempRemoteFile extends RemoteFile {
	private static final Pattern NAME_PATTERN = Pattern.compile("temp-([A-Za-z]+)-(.+)");
	private static final String NAME_FORMAT = "temp-%s-%s";

	private RemoteFile targetRemoteFile;
	
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
	 * Initializes a new randomly named temp file.
	 * 
	 * @throws StorageException
	 */
	public TempRemoteFile(RemoteFile targetRemoteFile) throws StorageException {
		super(String.format(NAME_FORMAT, CipherUtil.createRandomAlphabeticString(5), targetRemoteFile.getName()));
	}
	
	/**
	 * Returns the target remote file, i.e. the {@link RemoteFile} this
	 * temporary file will be renamed into, or was renamed from.
	 */
	public RemoteFile getTargetRemoteFile() {
		return targetRemoteFile;
	}

	@Override
	protected String validateName(String name) throws StorageException {
		Matcher matcher = NAME_PATTERN.matcher(name);

		if (!matcher.matches()) {
			throw new StorageException(name + ": remote filename pattern does not match: " + NAME_PATTERN.pattern() + " expected.");
		}
		
		try {
			targetRemoteFile = RemoteFile.createRemoteFile(matcher.group(2));
		}
		catch (Exception e) {
			throw new StorageException(name + ": remote filename pattern does not match: " + NAME_PATTERN.pattern() + " expected.");
		}

		return name;
	}
}
