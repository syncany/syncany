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
package org.syncany.watch.remote;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import org.syncany.db.CloneFile.Status;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.syncany.db.CloneChunk;
import org.syncany.db.CloneFile;
import org.syncany.db.Database;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FileUpdate {

    private String clientName = "";
    private long fileId = 0;
    private long version = 0;
    private String rootId = "";
    private String mergedRootId = null;
    private long mergedFileId = 0;
    private long mergedFileVersion = 0;
    private Date updated = null;
    private Status status = Status.UNKNOWN;
    private Date lastModified = null;
    private byte[] checksum = null;
    private long fileSize = 0;
    private boolean folder = false;
    private String name = "";
    private String path = "";
    
    /*
     * NOTE: Also update the "clone()" function when updating properties!!
     */
    
    
    /**
     * chunkIds (checksums)
     */
    private List<String> chunksAdded = new ArrayList<String>();
    /**
     * count (how many to remove from the end)
     */
    private int chunksRemoved = 0;
    /**
     * (index, chunk-id)
     */
    private Map<Integer, String> chunksChanged = new HashMap<Integer, String>();

    public FileUpdate() {
        // Nothing.
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public byte[] getChecksum() {
        return checksum;
    }

    public void setChecksum(byte[] checksum) {
        this.checksum = checksum;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getFileId() {
        return fileId;
    }

    public void setFileId(long fileId) {
        this.fileId = fileId;
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
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isFolder() {
        return folder;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public List<String> getChunksAdded() {
        return chunksAdded;
    }

    public void setChunksAdded(List<String> chunksAdded) {
        this.chunksAdded = chunksAdded;
    }

    public Map<Integer, String> getChunksChanged() {
        return chunksChanged;
    }

    public void setChunksChanged(Map<Integer, String> chunksChanged) {
        this.chunksChanged = chunksChanged;
    }

    public int getChunksRemoved() {
        return chunksRemoved;
    }

    public void setChunksRemoved(int chunksRemoved) {
        this.chunksRemoved = chunksRemoved;
    }

    public String getRootId() {
        return rootId;
    }

    public void setRootId(String rootId) {
        this.rootId = rootId;
    }

    public long getMergedFileId() {
        return mergedFileId;
    }

    public void setMergedFileId(long mergedFileId) {
        this.mergedFileId = mergedFileId;
    }

    public long getMergedFileVersion() {
        return mergedFileVersion;
    }

    public void setMergedFileVersion(long mergedFileVersion) {
        this.mergedFileVersion = mergedFileVersion;
    }

    public String getMergedRootId() {
        return mergedRootId;
    }

    public void setMergedRootId(String mergedRootId) {
        this.mergedRootId = mergedRootId;
    }

    public String getConflictedCopyName() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        
        return FileUtil.getBasename(getName())
            + " (" + getClientName()
            + (getClientName().endsWith("s") ? "'" : "'s")
            + " conflicting copy, "
            + dateFormat.format(new Date())
            + ")" + FileUtil.getExtension(getName(), true);        
    }
    
    public static FileUpdate fromCloneFile(CloneFile cf) {
    	

			List<String> chunksAdded = new ArrayList<String>();
			Map<Integer, String> chunksChanged = new HashMap<Integer, String>();
			int chunksRemoved = 0;
			
			if (cf.getStatus() == Status.RENAMED
					|| cf.getStatus() == Status.DELETED) {
				// Fressen.
			} else {
				CloneFile pv = cf.getPreviousVersion();

				// New string (first version): "1,2,3,4,..."
				if (pv == null) {
					List<CloneChunk> chunks = cf.getChunks();

					for (CloneChunk chunk : chunks)
						chunksAdded.add(chunk.getIdStr());
				}

				// Change string (not the first version!): "3=121,+122" or
				// "0=123,-5"
				else {
					List<CloneChunk> currentChunks = cf.getChunks();
					List<CloneChunk> previousChunks = pv.getChunks();
					int minChunkCount = (currentChunks.size() > previousChunks
							.size()) ? previousChunks.size() : currentChunks
							.size();

					// System.err.println("current chunks: "+cf.getChunks());
					// System.err.println("previo. chunks: "+pv.getChunks());
					// 1. Change
					for (int i = 0; i < minChunkCount; i++) {
						// Same chunk in both files; do nothing
						if (Arrays.equals(currentChunks.get(i).getChecksum(),
								previousChunks.get(i).getChecksum())) {
							continue;
						}
						chunksChanged.put(i, currentChunks.get(i).getIdStr());
					}

					// 2a. The current file has more chunks than the previous
					// one; add the rest
					if (currentChunks.size() > previousChunks.size()) {
						for (int i = previousChunks.size(); i < currentChunks
								.size(); i++) {
							chunksAdded.add(currentChunks.get(i).getIdStr());
						}
					}

					// 2b. The current file has fewer chunks than the previous
					// one; remove the rest
					else if (currentChunks.size() < previousChunks.size()) {
						chunksRemoved = (previousChunks.size() - currentChunks
												.size());
					}
				}
			} // create chunks-string

			// Write line
			Long updatedStr = (cf.getUpdated() == null) ? 0L : cf.getUpdated()
					.getTime();
			Long lastModifiedStr = (cf.getLastModified() == null) ? 0L : cf
					.getLastModified().getTime();

			FileUpdate fu = new FileUpdate();
			
			fu.setFileId(cf.getFileId());
			fu.setVersion(cf.getVersion());
			fu.setUpdated(cf.getUpdated());
			fu.setStatus(cf.getStatus());
			fu.setLastModified(cf.getLastModified());
			fu.setChecksum(cf.getChecksum());
			fu.setClientName(cf.getClientName());
			fu.setFileSize(cf.getFileSize());
			fu.setFolder(cf.isFolder());
			fu.setName(cf.getName());
			fu.setPath(cf.getPath());
			
			if (cf.getMergedTo() != null)
				fu.setMergedFileId(cf.getMergedTo().getFileId());
			if(cf.getMergedTo() != null)
				fu.setMergedFileVersion(cf.getMergedTo().getVersion());
			fu.setChunksAdded(chunksAdded);
			fu.setChunksChanged(chunksChanged);
			fu.setChunksRemoved(chunksRemoved);
			
			return fu;
	}
    
    @Override
    protected Object clone() {
        FileUpdate clone = new FileUpdate();
        
        clone.clientName = clientName;
        clone.fileId = fileId;
        clone.version = version;
        clone.rootId = rootId;
        clone.mergedRootId = mergedRootId;
        clone.mergedFileId = mergedFileId;
        clone.mergedFileVersion = mergedFileVersion;
        clone.updated = updated;
        clone.status = status;
        clone.lastModified = lastModified;
        clone.checksum = checksum;
        clone.fileSize = fileSize;
        clone.folder = folder;
        clone.name = name;
        clone.path = path;
        
        return clone;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FileUpdate other = (FileUpdate) obj;
        if (this.fileId != other.fileId) {
            return false;
        }
        if (this.version != other.version) {
            return false;
        }
        if (this.updated != other.updated && (this.updated == null || !this.updated.equals(other.updated))) {
            return false;
        }
        if (this.status != other.status) {
            return false;
        }
        if (this.lastModified != other.lastModified && (this.lastModified == null || !this.lastModified.equals(other.lastModified))) {
            return false;
        }
        if (!Arrays.equals(this.checksum, other.checksum)) {
            return false;
        }
        if (this.fileSize != other.fileSize) {
            return false;
        }
        if (this.folder != other.folder) {
            return false;
        }
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if ((this.path == null) ? (other.path != null) : !this.path.equals(other.path)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + (int) (this.fileId ^ (this.fileId >>> 32));
        hash = 79 * hash + (int) (this.version ^ (this.version >>> 32));
        hash = 79 * hash + (this.updated != null ? this.updated.hashCode() : 0);
        hash = 79 * hash + (this.status != null ? this.status.hashCode() : 0);
        hash = 79 * hash + (this.lastModified != null ? this.lastModified.hashCode() : 0);
        hash = 79 * hash + Arrays.hashCode(this.checksum);
        hash = 79 * hash + (int) (this.fileSize ^ (this.fileSize >>> 32));
        hash = 79 * hash + (this.folder ? 1 : 0);
        hash = 79 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 79 * hash + (this.path != null ? this.path.hashCode() : 0);
        return hash;
    }
   
    @Override
    public String toString() {
        return "Update[fileId=" + getFileId() + ", version=" + getVersion() + ", checksum=" + CloneFile.encodeChecksum(getChecksum()) + ", status=" + getStatus() + ", file=" + getPath() + "/" + getName() + ", folder=" + isFolder() + "]";
    }
}
