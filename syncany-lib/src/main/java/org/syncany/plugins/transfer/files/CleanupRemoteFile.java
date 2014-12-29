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

import org.syncany.plugins.transfer.StorageException;

/**
 * The transaction file only exists as an indicator to other clients a cleanup has occurred.
 * 
 * <p><b>Name pattern:</b> The name pattern of a cleanup file is
 * <b>cleanup-&lt;cleanupnumber&gt;</b>.
 * 
 * @author Pim Otte
 */
public class CleanupRemoteFile extends RemoteFile {
	private static final Pattern NAME_PATTERN = Pattern.compile("cleanup-([0-9]+)");
	private static final String NAME_FORMAT = "cleanup-%s";

	private long cleanupNumber;

	/**
	 * Initializes a new cleanup file, given a name. 
	 * 
	 * @param name cleanup file name; <b>must</b> always match the {@link #NAME_PATTERN} 
	 * @throws StorageException If the name is not match the name pattern
	 */
	public CleanupRemoteFile(String name) throws StorageException {
		super(name);
	}

	/**
	 * Initializes a new transaction file, given which cleanup has occurred 
	 * 
	 * @param remoteTransaction the remoteTransaction for which a file is needed
	 * @throws StorageException If the name is not match the name pattern
	 */
	public CleanupRemoteFile(long cleanupNumber) throws StorageException {
		super(String.format(NAME_FORMAT, Long.toString(cleanupNumber)));
	}

	@Override
	protected String validateName(String name) throws StorageException {
		Matcher matcher = NAME_PATTERN.matcher(name);

		if (!matcher.matches()) {
			throw new StorageException(name + ": remote filename pattern does not match: " + NAME_PATTERN.pattern() + " expected.");
		}

		cleanupNumber = Long.parseLong(matcher.group(1));

		return name;
	}

	public long getCleanupNumber() {
		return cleanupNumber;
	}
}
