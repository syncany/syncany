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
package org.syncany.db;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author Nikolai Hellwig
 *
 */
public class CloneFileTreeItemHashMap<K> extends HashMap<K, CloneFileTreeItem> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1392003401308677715L;

	/**
	 * Converts the CloneFileHashMap to a standard java list
	 * @return List of type {@link V}
	 */
	public List<CloneFileTreeItem> toList(){
		return new LinkedList<CloneFileTreeItem>(this.values());
	}
	
	public List<CloneFile> toCloneFileList(CloneFileFilter filter){
		List<CloneFileTreeItem> items = this.toList();
		List<CloneFile> list = new LinkedList<CloneFile>();
		for(CloneFileTreeItem item : items){
			if(filter.test(item.getCloneFile()))
				list.add(item.getCloneFile());
		}
		return list;
	}
}
