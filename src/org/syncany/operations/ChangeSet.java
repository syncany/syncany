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
package org.syncany.operations;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pheckel
 *
 */
public class ChangeSet {
	private List<String> changedFiles;  
	private List<String> newFiles;
	private List<String> deletedFiles;
	private List<String> unchangedFiles;
	
	public ChangeSet() {
		changedFiles = new ArrayList<String>();
		newFiles = new ArrayList<String>();
		deletedFiles = new ArrayList<String>();
		unchangedFiles = new ArrayList<String>();
	}
	
	public boolean hasChanges() {
		return changedFiles.size() > 0 
			|| newFiles.size() > 0
			|| deletedFiles.size() > 0;
	}
	
	public List<String> getChangedFiles() {
		return changedFiles;
	}
	
	public List<String> getNewFiles() {
		return newFiles;
	}
	
	public List<String> getDeletedFiles() {
		return deletedFiles;
	}	
	
	public List<String> getUnchangedFiles() {
		return unchangedFiles;
	}	
}
