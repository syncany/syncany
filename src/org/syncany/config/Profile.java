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
package org.syncany.config;

import java.io.File;

import org.syncany.connection.Downloader;
import org.syncany.connection.Uploader;
import org.syncany.watch.remote.RemoteWatcher;

/**
 * 
 * @author Philipp C. Heckel
 */
public class Profile {
	private static Profile instance;

	private String name;
	private Repository repository;
	private File root;

	private Uploader uploader;
	private Downloader downloader;
	private RemoteWatcher remoteWatcher;
	private Cache cache;

	private Profile() {
		name = "(unknown)";
		repository = new Repository();
		cache = new Cache();

		downloader = new Downloader(this);
		uploader = new Uploader(this);
		
		remoteWatcher = new RemoteWatcher(this);
	}

	
	public static Profile getInstance() {
		if (instance == null) {
			instance = new Profile();
		}
		return instance;
	}

	public File getRoot() {
		return root;
	}

	public Cache getCache() {
		return cache;
	}

	public Repository getRepository() {
		return repository;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setRoot(File rootFolder) {
		this.root = rootFolder;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public RemoteWatcher getRemoteWatcher() {
		return remoteWatcher;
	}

	public Uploader getUploader() {
		return uploader;
	}

	public Downloader getDownloader() {
		return downloader;
	}
}
