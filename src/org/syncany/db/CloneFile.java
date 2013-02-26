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

import org.syncany.config.Profile;
import org.syncany.config.Config;
import org.syncany.util.FileUtil;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base32;
import org.syncany.util.ArrayUtil;

/**
 * Represents a version of a file.
 * 
 * @author Philipp C. Heckel
 */
// @Table( uniqueConstraints={@UniqueConstraint(columnNames={"last_modified",
// "checksum"})} )
public class CloneFile implements Serializable,	Cloneable {

	private static final long serialVersionUID = 7370049494688304666L;
	
	private static final Logger logger = Logger.getLogger(CloneFile.class.getSimpleName());
	private static final Base32 b32 = new Base32();
	
	private CloneFile previous;
	private CloneFile next;

	/** Status of the file
	 * 
	 * <ul>
	 * <li>UNKNOWN
	 * <li>NEW: New file
	 * <lI>CHANGED: The file contents have changed. At least one chunk differs.
	 * <li>RENAMED: The file path or name has changed.
	 * <li>MERGED: The file history has been merged to a different file.
	 * </ul>
	 */
	public enum Status {
		UNKNOWN, NEW, CHANGED, RENAMED, DELETED, MERGED
	};

	/** TODO: Description not actual!
	 * 
	 * LOCAL: The file entry hasn't been propagated to the server yet 
	 * IN_UPDATE: The file entry should be included in the update-file, but not (yet) in
	 * the base file IN_BASE: The file entry should be included in the base-file
	 * (= complete DB dump)
	 */
	public enum SyncStatus {
		UNKNOWN, LOCAL, SYNCING, UPTODATE, CONFLICT
	};

	/**
	 * versionId of the root file; identifies the history of a file
	 */
	private Long fileId;
	private long version;
	private int profileId;
	private Date updated;
	private byte[] checksum;

	// FILE PROPERTIES
	private boolean isFolder;
	
	private CloneFile mergedTo;

	private String path;
	private String name;
	private long fileSize;
	private Date lastModified;
	private List<ChunkMapper> chunks;
	private Status status;
	private SyncStatus syncStatus;
	
	// TODO this should be a CloneClient instance
	private String clientName;

	public CloneFile() {
		this.fileId = new Random().nextLong();
		this.version = 1;
		this.chunks = new ArrayList<ChunkMapper>();
		this.status = Status.UNKNOWN;
		this.syncStatus = SyncStatus.UNKNOWN;

		this.checksum = null;
		this.name = "(unknown)";
		this.path = "(unknown)";
		this.clientName = Config.getInstance().getMachineName();
	}

	
	public CloneFile(File file) {
		this();

		if(file!=null) {
			setName(file.getName());
			setPath(FileUtil.getRelativeParentDirectory(Profile.getInstance().getRoot(), file));
			setFileSize(file.isDirectory() ? 0 : file.length());
			setLastModified(new Date(file.lastModified()));
			setIsFolder(file.isDirectory());
		}
	}

	public CloneFile getMergedTo() {
		return mergedTo;
	}

	public void setMergedTo(CloneFile mergedTo) {
		this.mergedTo = mergedTo;
	}

	public int getProfileId() {
		return profileId;
	}

	public void setProfileId(int profileId) {
		this.profileId = profileId;
	}

	public boolean isFolder() {
		return isFolder;
	}

	public void setIsFolder(boolean isFolder) {
		this.isFolder = isFolder;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public Long getFileId() {
		return fileId;
	}

	public void setFileId(Long fileId) {
		this.fileId = fileId;
	}

	public Date getUpdated() {
		return updated;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	public String getPath() {
		return Database.toFilesystemPath(path);
	}

	public void setPath(String path) {
		this.path = Database.toDatabasePath(path);
	}

	public SyncStatus getSyncStatus() {
		return syncStatus;
	}

	public void setSyncStatus(SyncStatus syncStatus) {
		this.syncStatus = syncStatus;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		System.out.println("CF: changing name from \""+this.name+"\" to \""+name+"\"");
		this.name = name;
	}

	public static String encodeChecksum(byte[] checksum) {
		if (checksum == null || ArrayUtil.isEmpty(checksum)) {
			return "";
		}

		return String.format("%s",
				b32.encodeAsString(checksum).replace("=", "")).toLowerCase();
	}

	public static byte[] decodeChecksum(String checksumStr) {
		if (checksumStr == null || checksumStr.trim().isEmpty()) {
			return null;
		}

		return b32.decode(checksumStr.toUpperCase());
	}

	/**
	 * Get relative path to the root dir.
	 */
	public String getRelativePath() {
		return FileUtil.getRelativePath(Profile.getInstance().getRoot(), getFile());
	}

	public String getRelativeParentDirectory() {
		return FileUtil.getRelativeParentDirectory(Profile.getInstance().getRoot(),
				getFile());
	}

	public String getAbsoluteParentDirectory() {
		return FileUtil.getAbsoluteParentDirectory(getFile());
	}

	public File getFile() {
		return FileUtil.getCanonicalFile(new File(Profile.getInstance().getRoot()
				+ File.separator + getPath() + File.separator + getName()));
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public byte[] getChecksum() {
		return checksum;
	}

	public String getChecksumStr() {
		return (checksum == null) ? "" : encodeChecksum(checksum);
	}

	public void setChecksum(byte[] checksum) {
		this.checksum = checksum;
	}

	public String getClientName() {
		return clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public CloneFile getPreviousVersion() {
		return getPrevious();
	}

	public List<CloneFile> getPreviousVersions() {
		// If we are the first, there are no others
		if (getVersion() == 1) {
			return new ArrayList<CloneFile>();
		}

		List<CloneFile> clist = new LinkedList<CloneFile>();
		
		CloneFile c = this.getPrevious();
		while (c != null) {
			clist.add(c);
			c = c.getPrevious();
		}

		return clist;

	}

	public List<CloneFile> getNextVersions() {
		List<CloneFile> clist = new LinkedList<CloneFile>();

		while (true) {
			CloneFile c = this.getNext();
			if (c != null)
				clist.add(c);
			else
				break;
		}

		return clist;
	}

	public List<CloneFile> getVersionHistory() {
		List<CloneFile> versions = new ArrayList<CloneFile>();

//		System.out.println("GetVersionHistory()");
//		System.out.println("getPreviousVersions: "+getPreviousVersions());
//		System.out.println("getNextVersions: "+getNextVersions());
		
		versions.addAll(getPreviousVersions());
		versions.add(this);
		versions.addAll(getNextVersions());

		return versions;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public List<CloneChunk> getChunks() {
		List<CloneChunk> chunkList = new ArrayList<CloneChunk>();

		for (ChunkMapper mapper : chunks) {
			chunkList.add(mapper.getChunk());
		}

		return chunkList;
	}

	public CloneChunk getChunk(int index) {
		return chunks.get(index).getChunk();
	}

	public synchronized void setChunks(List<CloneChunk> chunkList) {
		chunks = new ArrayList<ChunkMapper>();

		for (int i = 0; i < chunkList.size(); i++) {
			chunks.add(new ChunkMapper(this, chunkList.get(i), i));
		}
	}

	public synchronized void setChunk(int index, CloneChunk chunk) {
		chunks.set(index, new ChunkMapper(this, chunk, index));
	}

	public void addChunk(CloneChunk chunk) {
		chunks.add(new ChunkMapper(this, chunk, chunks.size()));
	}

	public void addChunks(List<CloneChunk> chunkList) {
		for (CloneChunk chunk : chunkList) {
			addChunk(chunk);
		}
	}

	public synchronized void removeChunks(int count) {
		int minIndex = (chunks.size() - count - 1 < 0) ? 0 : chunks.size()
				- count - 1;
		int maxIndex = (chunks.size() - 1 < 0) ? 0 : chunks.size() - 1;

		for (int i = maxIndex; i == minIndex; i--) {
			chunks.remove(i);
		}
	}

	@Override
	public int hashCode() {
		int hash = 0;
		hash += (fileId != null ? fileId.hashCode() : 0);
		hash += version;
		return hash;
	}

	@Override
	public Object clone() {
		try {
			CloneFile clone = (CloneFile) super.clone();

			clone.fileId = getFileId();
			clone.updated = getUpdated();
			clone.checksum = (getChecksum() != null) ? Arrays.copyOf(
					getChecksum(), getChecksum().length) : null;
			clone.lastModified = (getLastModified() != null) ? new Date(
					getLastModified().getTime()) : null;
			// clone.profile = getProfile(); // POINTER; No Copy!
			clone.profileId = getProfileId();
//			clone.root = getRoot(); // POINTER; No Copy!
//			clone.rootId = getRootId();
			clone.isFolder = isFolder();
			clone.path = new String(getPath()); // Copy
			clone.name = new String(getName()); // Copy
			clone.clientName = new String(getClientName()); // Copy
			clone.fileSize = getFileSize();
			clone.status = getStatus();
			clone.syncStatus = getSyncStatus();
			clone.mergedTo = getMergedTo(); // POINTER

			// Copies
			clone.setChunks(getChunks()); // Copy (due to mapper)

			return clone;
		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
			throw new RuntimeException(ex);
		}
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof CloneFile)) {
			return false;
		}

		CloneFile other = (CloneFile) object;

		if (other.fileId == null || this.fileId == null) {
			return false;
		}

		return other.fileId.equals(this.fileId)
				&& other.version == this.version;
	}

	// Comment this method to get object id
	@Override
	public String toString() {
		return "CloneFile[id=" + fileId + ",version=" + version + ", path="
				+ getPath() + ", name=" + getName() + "]";
	}
	
	public String print() {
		return "CloneFile[id=" + fileId + ",version=" + version + ", path="
				+ getPath() + ", name=" + getName() + ", status= "+status+"]";
	}

	public CloneFile getPrevious() {
		return previous;
	}

	public void setPrevious(CloneFile previous) {
		this.previous = previous;
	}

	public CloneFile getNext() {
		return next;
	}

	public void setNext(CloneFile next) {
		this.next = next;
	}
	
	public String getFullPath(){
		return (this.getPath().isEmpty()) ? Database.toDatabasePath(this
				.getName()) : Database.toDatabasePath(this.getPath()
				+ File.separator + this.getName());
	}
	
	public void remove(){
		Database.getInstance().removeFile(this);
	}

	public void merge(){
		Database.getInstance().merge(this);
	}
	
	public void persist(){
		Database.getInstance().persist(this);
	}
}
