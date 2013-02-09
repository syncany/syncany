/*
 * Syncany
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
package org.syncany.experimental.trash;

import java.util.List;
import java.util.Map;

import org.syncany.experimental.db.ChunkEntry;
import org.syncany.experimental.db.FileVersion;
import org.syncany.experimental.db.MultiChunkEntry;



public class RepoDBVersion {
	long currentVersion;
	List<FileVersion> fileVersions;
	List<ChunkEntry> metaChunks;
	List<MultiChunkEntry> metaMultiChunks;
	public List<FileVersion> getFileVersions() {
		return fileVersions;
	}
	public void setFileVersions(List<FileVersion> fileVersions) {
		this.fileVersions = fileVersions;
	}
	
	public List<MultiChunkEntry> getMetaMultiChunks() {
		//return new List<blablabla>
		return null;
	}

}
