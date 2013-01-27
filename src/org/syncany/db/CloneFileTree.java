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
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import org.syncany.Constants;
import org.syncany.config.Profile;
import org.syncany.exceptions.CloneTreeException;
import org.syncany.util.ByteArray;

/**
 * 
 * @author Nikolai Hellwig
 * 
 */
public class CloneFileTree implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5840745574409649485L;

	private CloneFileTreeItem root;
	private Hashtable<Long, CloneFile> cloneFileMap;
	private Hashtable<ByteArray, List<CloneFile>> checksumFileMap;
	List<CloneFile> newAddedFiles;

	public CloneFileTree() {
		root = new CloneFileTreeItem("ROOT");
		cloneFileMap = new Hashtable<Long, CloneFile>();
		checksumFileMap = new Hashtable<ByteArray, List<CloneFile>>();
		newAddedFiles = new LinkedList<CloneFile>();
	}

	public synchronized CloneFileTreeItem getFileByPath(String path) {
		String[] parts = path.split(Constants.DATABASE_FILE_SEPARATOR);

		CloneFileTreeItem item = root;
		for (int i = 0; i < parts.length; i++) {
			item = item.getChild(parts[i]);
			if (item == null)
				return null;
		}

		return item;
	}

	public synchronized CloneFile getFileById(long fileId) {
		return cloneFileMap.get(fileId);
	}

	// TODO: Optimize this (slow in case of many different versions)
	public synchronized CloneFile getFileByIdAndVersion(long fileId, long version) {
		CloneFile c = getFileById(fileId);
		if (c == null)
			return null;

		while (true) {
			if (c.getVersion() == version)
				break;

			c = c.getPrevious();

			if (c == null)
				return null;
		}

		return c;
	}

	// TODO: CHECK IF CORRECT
	public synchronized int getVersionCount() {
		return cloneFileMap.size();
	}

	public synchronized void updateFile(CloneFile c) {
		cloneFileMap.put(c.getFileId(), c);
		newAddedFiles.add(c);
		
		//if(c.getClientName().equals(Profile.getInstance().getName()))
		//	Database.getInstance().incrementVersion();

		if (!c.isFolder()){
			List<CloneFile> list = checksumFileMap.get(new ByteArray(c.getChecksum()));
			if (list == null){
				list = new LinkedList<CloneFile>();
				checksumFileMap.put(new ByteArray(c.getChecksum()), list);
			}
			list.remove(c);
			list.add(c);
		}
		
		updateCloneFileInTree(c);
	}

	private synchronized void updateCloneFileInTree(CloneFile c) {
		String p = c.getFullPath();
		String[] parts = p.split(Constants.DATABASE_FILE_SEPARATOR);

		CloneFileTreeItem item = root;
		for (int i = 0; i < parts.length - 1; i++) {
			CloneFileTreeItem newitem = item.getChild(parts[i]);
			if (newitem == null) {
				newitem = item.addChild(parts[i], new CloneFileTreeItem(parts[i]));
			}

			item = newitem;
		}

		CloneFileTreeItem testItem = item.getChild(parts[parts.length - 1]);
		if (testItem != null) {
			testItem.setCloneFile(c);
		} else
			item.addChild(parts[parts.length - 1], new CloneFileTreeItem(c));
	}

	public synchronized void removeCloneFile(CloneFile c) {
		cloneFileMap.remove(c.getFileId());
		if (!c.isFolder()){
			checksumFileMap.remove(new ByteArray(c.getChecksum()));
		}
		removeFromTree(c);
	}

	private synchronized void removeFromTree(CloneFile c) {
		String p = c.getFullPath();
		String[] parts = p.split(Constants.DATABASE_FILE_SEPARATOR);

		CloneFileTreeItem item = root;
		for (int i = 0; i < parts.length - 1; i++) {
			CloneFileTreeItem newitem = item.getChild(parts[i]);
			if (newitem == null) {
				newitem = item.addChild(parts[i], new CloneFileTreeItem(parts[i]));
			}

			item = newitem;
		}

		item.remove(parts[parts.length - 1]);
	}

	public synchronized List<CloneFile> getFilesByChecksum(byte[] checksum, CloneFileFilter filter) {
		List<CloneFile> files = checksumFileMap.get(checksum);

		if (files == null || files.size() < 1) {
			return new LinkedList<CloneFile>();
		}

		List<CloneFile> ret = new LinkedList<CloneFile>();
		for (CloneFile f : files) {
			if (filter.test(f))
				ret.add(f);
		}

		return ret;
	}

	public synchronized List<CloneFile> getAllFiles() {
		return root.getCloneFileChildren(Constants.DELETED_MERGED_FILTER);
	}

	public synchronized Collection<CloneFile> getNewAddedFile() {
		return newAddedFiles;
	}

	public synchronized void clearNewAddedList() {
		newAddedFiles.clear();
	}

	public synchronized List<CloneFile> getHistory() {
		return root.getCloneFileChildren(null);
	}

	public synchronized void addCloneFile(CloneFile c) throws CloneTreeException {
		String p = c.getFullPath();
		String[] parts = p.split(Constants.DATABASE_FILE_SEPARATOR);
		
		CloneFileTreeItem item = root;
		for (int i = 0; i < parts.length - 1; i++) {
			CloneFileTreeItem newitem = item.getChild(parts[i]);
			if (newitem == null) {
				newitem = item.addChild(parts[i], new CloneFileTreeItem(parts[i]));
			}

			item = newitem;
		}

		CloneFileTreeItem testItem = item.getChild(parts[parts.length - 1]);
		if (testItem != null) {
			if (testItem.getCloneFile() != null && testItem.getCloneFile().getChecksum() != null) // isDirectory!
				throw new CloneTreeException("CloneFile exists in tree already");
			else
				testItem.setCloneFile(c);
		} else
			item.addChild(parts[parts.length - 1], new CloneFileTreeItem(c));

		cloneFileMap.put(c.getFileId(), c);
		newAddedFiles.add(c);
		//if(c.getClientName().equals(Profile.getInstance().getName()))
		//	Database.getInstance().incrementVersion();

		if (!c.isFolder()) { // it doesn't make sense to save a folder which has in fact no checksum
			
			/* 
			 * TODO: Null-Pointer if c has no checksum!
			 */
			List<CloneFile> clist = checksumFileMap.get(new ByteArray(c.getChecksum()));
			if (clist == null) {
				clist = new LinkedList<CloneFile>();
				checksumFileMap.put(new ByteArray(c.getChecksum()), clist);
			}
			clist.add(c);
		}
	}

	@Override
	public synchronized String toString() {
		final StringBuffer sb = new StringBuffer();

		root.deepSearch(0, root.getDirectChildren(), new DeepSearchOperation<CloneFileTreeItem>() {
			@Override
			public void execute(CloneFileTreeItem o, int level) {
				CloneFile c = o.getCloneFile();

				String tabs = "";
				for (int i = 0; i < level; i++)
					tabs += "\t";
				if (c != null)
					sb.append(tabs + level + ": " + c.getName() + ", " + c.getVersion() + ", " + c.getStatus() + ", " + c.getPreviousVersion()
							+ "\r\n");
				else
					sb.append(tabs + level + ": " + o.getName() + "\r\n");
			}
		});

		return sb.toString();
	}
}
