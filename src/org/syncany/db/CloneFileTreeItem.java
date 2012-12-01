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

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author Nikolai Hellwig
 *
 */
public class CloneFileTreeItem implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8540309509807210489L;
	
	private CloneFile cloneFile;
	private String name;
	
	
	public void setCloneFile(CloneFile cloneFile) {
		this.cloneFile = cloneFile;
	}

	private CloneFileTreeItemHashMap<String> children;
	
	public CloneFileTreeItem(String name){
		children = new CloneFileTreeItemHashMap<String>();
		this.name = name;
	}
	
	public CloneFileTreeItem(CloneFile c){
		this(c.getName());
		this.cloneFile = c;
	}
	
	public List<CloneFileTreeItem> getDirectChildren(){
		return children.toList();
	}
	
	public List<CloneFile> getDirectCloneFileChildren(CloneFileFilter filter){
		return children.toCloneFileList(filter);
	}
	
	public List<CloneFileTreeItem> getChildren(){
		List<CloneFileTreeItem> children = new LinkedList<CloneFileTreeItem>();
		deepSearch(children, getDirectChildren());
		return children;
	}
	
	public List<CloneFile> getCloneFileChildren(CloneFileFilter filter){
		List<CloneFile> children = new LinkedList<CloneFile>();
		deepSearchCloneFile(children, getDirectChildren(), filter);
		return children;
	}
	
	public void deepSearch(List<CloneFileTreeItem> list, List<CloneFileTreeItem> children){
		for(CloneFileTreeItem item : children){
			list.add(item);
			List<CloneFileTreeItem> items = item.getDirectChildren();
			if(items != null && items.size() > 0)
				deepSearch(list, items);
		}
	}
	
	public void deepSearch(int level, List<CloneFileTreeItem> children, DeepSearchOperation<CloneFileTreeItem> operation){
		for(CloneFileTreeItem item : children){
			operation.execute(item, level);
			List<CloneFileTreeItem> items = item.getDirectChildren();
			if(items != null && items.size() > 0)
				deepSearch(level + 1, items, operation);
		}
	}
	
	public void deepSearchCloneFile(List<CloneFile> list, List<CloneFileTreeItem> children, CloneFileFilter filter){
		for(CloneFileTreeItem item : children){
			if(filter == null || filter.test(item.getCloneFile()))
				list.add(item.getCloneFile());
			List<CloneFileTreeItem> items = item.getDirectChildren();
			if(items != null && items.size() > 0)
				deepSearchCloneFile(list, items, filter);
		}
	}

	public CloneFileTreeItem getChild(String name) {
		return children.get(name);
	}
	
	public CloneFileTreeItem addChild(String name, CloneFileTreeItem item){
		children.put(name, item);
		return item;
	}
	
	public void remove(String key){
		children.remove(key);
	}

	public CloneFile getCloneFile() {
		return cloneFile;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
}
