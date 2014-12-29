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
package org.syncany.operations;

import java.util.TreeSet;

import org.simpleframework.xml.ElementList;

/**
 * A change set represents the result of a comparison of two file trees, either 
 * by comparing a local file tree with the local database, or by comparing the remote
 * database with the local database.
 * 
 * <p>It contains several lists, indicating new, changed, deleted and unchanged files.
 * File paths are stored relative to the root Syncany directory.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class ChangeSet {
	@ElementList(name = "changedFiles", entry = "file", required = false)
	private TreeSet<String> changedFiles;  

	@ElementList(name = "newFiles", entry = "file", required = false)
	private TreeSet<String> newFiles;
	
	@ElementList(name = "deletedFiles", entry = "file", required = false)
	private TreeSet<String> deletedFiles;
	
	@ElementList(name = "unchangedFiles", entry = "file", required = false)
	private TreeSet<String> unchangedFiles;
	
	public ChangeSet() {
		changedFiles = new TreeSet<String>();
		newFiles = new TreeSet<String>();
		deletedFiles = new TreeSet<String>();
		unchangedFiles = new TreeSet<String>();
	}
	
	/**
	 * Returns <tt>true</tt> if files have been added, changed or
	 * deleted by checking the size of {@link #getNewFiles()}, {@link #getChangedFiles()}
	 * and {@link #getDeletedFiles()}.
	 */
	public boolean hasChanges() {
		return changedFiles.size() > 0 
			|| newFiles.size() > 0
			|| deletedFiles.size() > 0;
	}
	
	// TODO [low] This is ugly. Use unmutable lists.
	public TreeSet<String> getChangedFiles() {
		return changedFiles;
	}
	
	public TreeSet<String> getNewFiles() {
		return newFiles;
	}
	
	public TreeSet<String> getDeletedFiles() {
		return deletedFiles;
	}	
	
	public TreeSet<String> getUnchangedFiles() {
		return unchangedFiles;
	}
}
