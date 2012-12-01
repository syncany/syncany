/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.watch.remote.files;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Profile;
import org.syncany.config.Repository;
import org.syncany.config.Settings;
import org.syncany.db.CloneClient;
import org.syncany.db.Database;

/**
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class StructuredFileList {
	private Repository repository;
	private Map<String, RemoteFile> fileList;

	private TreeMap<Long, UpdateFile> localUpdateFiles;
	private Map<String, UpdateFile> remoteUpdateFiles;

	public StructuredFileList(Repository repository, Map<String, RemoteFile> fileList) {
		this.repository = repository;
		this.fileList = fileList;

		this.localUpdateFiles = new TreeMap<Long, UpdateFile>();
		this.remoteUpdateFiles = new HashMap<String, UpdateFile>();

		parseList();
	}

	private void parseList() {
		for (RemoteFile rf : fileList.values()) {
			try {
				// Is update file?
				if (rf.getName().startsWith(UpdateFile.PREFIX)) {
					UpdateFile newUpdateFile = UpdateFile.createUpdateFile(repository, rf);

					// Local?
					if (newUpdateFile.getMachineName().equals(Settings.getInstance().getMachineName())) {
						localUpdateFiles.put(newUpdateFile.getLastUpdate().getTime(), newUpdateFile);
					}

					// Remote?
					else {
						// Add if newer
						CloneClient client = Database.getInstance().getClient(newUpdateFile.getMachineName(), false);
						Logger.getLogger("").log(Level.INFO, "Last Update " + ((client != null) ? client.getLastUpdate() : " no update"));
						if (client == null || client.getLastUpdate() == null || newUpdateFile.getLastUpdate().after(client.getLastUpdate())) {
							remoteUpdateFiles.put(newUpdateFile.getName(), newUpdateFile);
						}
					}
				}
			} catch (Exception e) {
				// Ignore file
				System.err.println(e.getMessage());
				continue;
			}
		}

	}
	
	public TreeMap<Long, UpdateFile> getLocalUpdateFiles() {
		return localUpdateFiles;
	}

	public Collection<UpdateFile> getLocalUpdateFilesList() {
		return localUpdateFiles.values();
	}

	public Map<String, UpdateFile> getRemoteUpdateFiles() {
		return remoteUpdateFiles;
	}
}
