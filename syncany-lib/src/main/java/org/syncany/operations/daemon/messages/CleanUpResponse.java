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
package org.syncany.operations.daemon.messages;

import java.io.File;
import java.util.ArrayList;

import org.simpleframework.xml.ElementList;

public class CleanUpResponse extends Response {
	@ElementList(required = false, entry = "new")
	private ArrayList<String> newFiles;
	
	@ElementList(required = false, entry = "changed")
	private ArrayList<String> changedFiles;
	
	@ElementList(required = false, entry = "deleted")
	private ArrayList<String> deletedFiles;
	
	public CleanUpResponse() {
		// Required default constructor!
	}
	
	public CleanUpResponse(int requestId) {
		super(200, requestId, null);
	}	
	
	public ArrayList<String> getNewFiles() {
		return newFiles;
	}
	public void setNewFiles(ArrayList<String> newFiles) {
		this.newFiles = newFiles;
	}
	public ArrayList<String> getChangedFiles() {
		return changedFiles;
	}
	public void setChangedFiles(ArrayList<String> changedFiles) {
		this.changedFiles = changedFiles;
	}
	public ArrayList<String> getDeletedFiles() {
		return deletedFiles;
	}
	public void setDeletedFiles(ArrayList<String> deletedFiles) {
		this.deletedFiles = deletedFiles;
	}
	
	public void addNewFile(String file){
		if (newFiles == null) newFiles = new ArrayList<>();
		newFiles.add(file);
	}
	public void addChangedFile(String file){
		if (changedFiles == null) changedFiles = new ArrayList<>();
		changedFiles.add(file);
	}
	public void addDeletedFile(String file){
		if (deletedFiles == null) deletedFiles = new ArrayList<>();
		deletedFiles.add(file);
	}
}
