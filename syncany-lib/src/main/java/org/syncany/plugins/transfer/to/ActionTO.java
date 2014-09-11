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
package org.syncany.plugins.transfer.to;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.files.RemoteFile;
import org.syncany.plugins.transfer.files.TempRemoteFile;

/**
 * A TransactionActionTO describes a single action on a file, which is to be bundled
 * with other actions to describe a full transaction.
 * 
 * @author Pim Otte
 */
@Root(name = "transactionAction")
public class ActionTO {
	private static final Logger logger = Logger.getLogger(ActionTO.class.getSimpleName());

	public static final String TYPE_UPLOAD = "UPLOAD";
	public static final String TYPE_DELETE = "DELETE";

	@Element(name = "type", required = true)
	private String type;

	@Element(name = "remoteLocation", required = true)
	private String remoteLocation;

	@Element(name = "remoteTempLocation", required = true)
	private String remoteTempLocation;

	@Element(name = "localTempLocation", required = false)
	private String localTempLocation;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setRemoteLocation(RemoteFile remoteFile) {
		remoteLocation = remoteFile.getName();
	}

	public RemoteFile getRemoteFile() throws StorageException {
		return RemoteFile.createRemoteFile(remoteLocation);
	}

	public void setRemoteTempLocation(TempRemoteFile tempRemoteFile) {
		remoteTempLocation = tempRemoteFile.getName();
	}

	public TempRemoteFile getTempRemoteFile() {
		try {
			return RemoteFile.createRemoteFile(remoteTempLocation, TempRemoteFile.class);
		}
		catch (StorageException e) {
			logger.log(Level.INFO, "Invalid remote temporary filename: " + remoteTempLocation);
			return null;
		}
	}

	public void setLocalTempLocation(File file) {
		localTempLocation = file.getAbsolutePath();
	}

	public File getLocalTempLocation() {
		return new File(localTempLocation);
	}

	@Override
	public String toString() {
		return "ActionTO [type=" + type + ", remoteLocation=" + remoteLocation + ", remoteTempLocation=" + remoteTempLocation
				+ ", localTempLocation=" + localTempLocation + "]";
	}		
}
