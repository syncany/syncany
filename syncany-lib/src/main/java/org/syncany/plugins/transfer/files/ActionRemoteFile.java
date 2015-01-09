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
 * Action remote files represent a running transfer operation on the remote storage. Transfer
 * operations are operations that modify the repository and/or are relevant for the 
 * consistency of the local directory or the remote repository. Action files have no content,
 * they are just markers and exist only while an operation is running.
 * 
 * <p><b>Name pattern:</b> The name pattern of an action file is
 * <b>action-&lt;operationname&gt;-&lt;machinename&gt;-&lt;timestamp&gt;</b>. Initializing an 
 * instance with a non-matching name will throw an exception.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class ActionRemoteFile extends RemoteFile {
	private static final Pattern NAME_PATTERN = Pattern.compile("action-(up|down|cleanup|restore)-([^-]+)-(\\d+)");
	private static final String NAME_FORMAT = "action-%s-%s-%010d";

	private String operationName;
	private String clientName;
	private long timestamp;

	public ActionRemoteFile(String name) throws StorageException {
		super(name);
	}

	public ActionRemoteFile(String operationName, String clientName, long timestamp) throws StorageException {
		super(String.format(NAME_FORMAT, operationName, clientName, timestamp));
	}

	public String getOperationName() {
		return operationName;
	}

	public String getClientName() {
		return clientName;
	}

	public long getTimestamp() {
		return timestamp;
	}
	
	@Override
	protected String validateName(String name) throws StorageException {
		Matcher matcher = NAME_PATTERN.matcher(name);

		if (!matcher.matches()) {
			throw new StorageException(name + ": remote database filename pattern does not match: " + NAME_PATTERN.pattern() + " expected.");
		}

		operationName = matcher.group(1);
		clientName = matcher.group(2);
		timestamp = Long.parseLong(matcher.group(3));

		return name;
	}
}
