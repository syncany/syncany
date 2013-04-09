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
package org.syncany.watch.remote.files;

import org.syncany.db.CloneChunk;
import org.syncany.db.CloneFile;
import org.syncany.db.CloneFile.Status;
import org.syncany.util.StringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.syncany.config.Repository;
import org.syncany.db.Database;
import org.syncany.watch.remote.FileHistoryPart;
import org.syncany.watch.remote.FileUpdate;

/**
 * Represents an update file of one client. File format is CSV. It is a remote
 * file with fixed syntax. Once it's downloaded to a local file it can be read
 * and written.
 * 
 * <pre>
 * fileId,version,versionId,updated,status,lastModified,checksum,fileSize,name,path,chunks
 * 1282850694262,1,12828..,...,NEW,...,-309591037,11,AAA,folder,CHUNKS_SYNTAX
 * 1282850694262,2,12828..,...,RENAMED,...,-309591037,11,AAA-renamed,folder,CHUNKS_SYNTAX
 * ...
 * </pre>
 * 
 * <pre>
 * +CHUNK_ID                Add chunk with checksum CHUNK_ID to the end
 * -CHUNKS_COUNT            Remove CHUNKS_COUNT chunks from the end
 * CHUNK_INDEX=CHUNK_ID     ...
 * </pre>
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class UpdateFile extends DatedClientRemoteFile {
	public static final String PREFIX = "update";
	public static final Pattern FILE_PATTERN = Pattern.compile("^" + PREFIX
			+ "-([A-Z, a-z, 0-9, -]+)-(\\d+)$");
	public static final String FILE_FORMAT = PREFIX + "-%s-%d";

	/*
	 * file versions to be saved in the file
	 */
	private List<CloneFile> versions;

	/**
	 * (file, (version, update)) the version-map (tree-map) is sorted ascending!
	 */
	private Map<Long, FileHistoryPart> updates;

	public UpdateFile(Repository repository, String clientName, Date lastUpdate) {
		super(repository, PREFIX, clientName, lastUpdate);

		this.updates = new HashMap<Long, FileHistoryPart>();
	}

	public static UpdateFile createUpdateFile(Repository repository,
			RemoteFile remoteFile) {
		// Check file
		Matcher m = FILE_PATTERN.matcher(remoteFile.getName());

		if (!m.matches()) {
			throw new IllegalArgumentException(
					"Given remote file is not a profile file: " + remoteFile);
		}

		return new UpdateFile(repository, m.group(1), new Date(Long.parseLong(m
				.group(2))));
	}

	@Override
	public void read(File file) throws IOException, ClassNotFoundException {
		read(file, true);
	}

	public void read(File file, boolean gzipped) throws IOException, ClassNotFoundException{
		ObjectInputStream dis = (gzipped) ? new ObjectInputStream(
				new GZIPInputStream(new FileInputStream(file)))
				: new ObjectInputStream(new FileInputStream(file));

		while (dis.available() > 0) {
			FileUpdate update = new FileUpdate();

			//update.setRootId(dis.readUTF());
			update.setFileId(dis.readLong());
			update.setVersion(dis.readLong());
			update.setUpdated(new Date(dis.readLong()));
			update.setStatus(Status.valueOf(dis.readUTF()));
			update.setLastModified(new Date(dis.readLong()));
			update.setChecksum(CloneFile.decodeChecksum(dis.readUTF()));
			update.setClientName(dis.readUTF());
			update.setFileSize(dis.readLong());
			update.setFolder(dis.readBoolean());
			update.setName(dis.readUTF());
			update.setPath(Database.toFilesystemPath(dis.readUTF()));

			// Merged Into
			String mergedFileId = dis.readUTF();
			//String mergedRootId = dis.readUTF();
			String mergedFileVersion = dis.readUTF();

			if (mergedFileId != null && !mergedFileId.isEmpty()) {
				//update.setMergedRootId(mergedRootId);
				update.setMergedFileId(Long.parseLong(mergedFileId));
				update.setMergedFileVersion(Long.parseLong(mergedFileVersion));
			}

			// Parse chunks-value
			String sChunks = (String)dis.readObject();
			String[] chunks = sChunks.split(",");

			List<String> chunksAdded = new ArrayList<String>();
			Map<Integer, String> chunksChanged = new HashMap<Integer, String>();
			int chunksRemoved = 0;

			// Only do detailed checks if the chunks have changed
			if (!(chunks.length == 1 && chunks[0].isEmpty())) {
				// 1a. First version: "123,124,125,..."
				if (update.getVersion() == 1) {
					for (String chunk : chunks) {
						if (chunk.isEmpty()) {
							continue;
						}

						chunksAdded.add(chunk);
					}
				}

				// 1b. Not the first version: "0=123,+124,+125"
				else {
					for (String chunk : chunks) {
						if (chunk.isEmpty()) {
							continue;
						}

						String pos1 = chunk.substring(0, 1);

						if ("+".equals(pos1)) {
							chunksAdded.add(chunk.substring(1, chunk.length()));
						}

						else if ("-".equals(pos1)) {
							chunksRemoved = Integer.parseInt(chunk.substring(1,
									chunk.length()));
						}

						else {
							String[] changeChunk = chunk.split("=");
							chunksChanged.put(Integer.parseInt(changeChunk[0]),
									changeChunk[1]);
						}
					}
				}
			}

			update.setChunksAdded(chunksAdded);
			update.setChunksRemoved(chunksRemoved);
			update.setChunksChanged(chunksChanged);

			// Add to map
			add(update);
		}

		dis.close();
	}

	@Override
	public void write(File file) throws IOException {
		write(file, true);
	}

	public void write(File file, boolean gzipped) throws IOException {
		ObjectOutputStream dos = getObjectOutputStream(file, gzipped);

		for (CloneFile cf : versions) {
			// Create 'chunks' string
			List<String> chunksStr = new ArrayList<String>();

			if (cf.getStatus() == Status.RENAMED
					|| cf.getStatus() == Status.DELETED) {
				// Fressen.
			} else {
				CloneFile pv = cf.getPreviousVersion();

				// New string (first version): "1,2,3,4,..."
				if (pv == null) {
					List<CloneChunk> chunks = cf.getChunks();

					for (CloneChunk chunk : chunks)
						chunksStr.add(chunk.getIdStr());
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

						chunksStr
								.add(i + "=" + currentChunks.get(i).getIdStr());
					}

					// 2a. The current file has more chunks than the previous
					// one; add the rest
					if (currentChunks.size() > previousChunks.size()) {
						for (int i = previousChunks.size(); i < currentChunks
								.size(); i++) {
							chunksStr
									.add("+" + currentChunks.get(i).getIdStr());
						}
					}

					// 2b. The current file has fewer chunks than the previous
					// one; remove the rest
					else if (currentChunks.size() < previousChunks.size()) {
						chunksStr
								.add("-"
										+ (previousChunks.size() - currentChunks
												.size()));
					}
				}
			} // create chunks-string

			// Write line
			Long updatedStr = (cf.getUpdated() == null) ? 0L : cf.getUpdated()
					.getTime();
			Long lastModifiedStr = (cf.getLastModified() == null) ? 0L : cf
					.getLastModified().getTime();

//			dos.writeUTF(cf.getRootId());
			dos.writeLong(cf.getFileId());
			dos.writeLong(cf.getVersion());
			dos.writeLong(updatedStr);
			dos.writeUTF(cf.getStatus().toString());
			dos.writeLong(lastModifiedStr);
			dos.writeUTF(cf.getChecksumStr());
			dos.writeUTF(cf.getClientName());
			dos.writeLong(cf.getFileSize());
			dos.writeBoolean(cf.isFolder());
			dos.writeUTF(cf.getName());
			dos.writeUTF(Database.toDatabasePath(cf.getPath()));
//			dos.writeUTF((cf.getMergedTo() != null) ? cf.getMergedTo()
//					.getRootId() : "");
			dos.writeUTF((cf.getMergedTo() != null) ? Long.toString(cf
					.getMergedTo().getFileId()) : "");
			dos.writeUTF((cf.getMergedTo() != null) ? Long.toString(cf
					.getMergedTo().getVersion()) : "");
			
			// changed due to utf data exception
			dos.writeObject(StringUtil.join(chunksStr, ","));
		}

		dos.close();
	}

	public ObjectOutputStream getObjectOutputStream(File file, boolean gzipped)
			throws IOException, FileNotFoundException {
		ObjectOutputStream dos = (gzipped) ? new ObjectOutputStream(
				new GZIPOutputStream(new FileOutputStream(file)))
				: new ObjectOutputStream(new FileOutputStream(file));
		return dos;
	}
	
	public static void append(File file, boolean gzipped, ObjectOutputStream dos)
			throws IOException, ClassNotFoundException {

		ObjectInputStream dis = (gzipped) ? new ObjectInputStream(
				new GZIPInputStream(new FileInputStream(file)))
				: new ObjectInputStream(new FileInputStream(file));

		while (dis.available() > 0) {
			//dos.writeUTF(dis.readUTF()); // ROOT ID
			dos.writeLong(dis.readLong());
			dos.writeLong(dis.readLong());
			dos.writeLong(dis.readLong());
			dos.writeUTF(dis.readUTF());
			dos.writeLong(dis.readLong());
			dos.writeUTF(dis.readUTF());
			dos.writeUTF(dis.readUTF());
			dos.writeLong(dis.readLong());
			dos.writeBoolean(dis.readBoolean());
			dos.writeUTF(dis.readUTF());
			dos.writeUTF(dis.readUTF());
			//dos.writeUTF(dis.readUTF()); // mergedTO
			dos.writeUTF(dis.readUTF());
			dos.writeUTF(dis.readUTF());
			dos.writeObject(dis.readObject());
		}
	}

	public void add(FileUpdate update) {
		FileHistoryPart fileVersionUpdates = updates.get(update.getFileId());

		if (fileVersionUpdates == null) {
			fileVersionUpdates = new FileHistoryPart(getMachineName(),
					update.getFileId());
		}

		fileVersionUpdates.add(update);

		updates.put(update.getFileId(), fileVersionUpdates);
	}

	public FileHistoryPart getFileUpdates(long fileId) {
		return updates.get(fileId);
	}

	public FileUpdate getFileUpdate(long fileId, long version) {
		FileHistoryPart versionUpdates = updates.get(fileId);

		if (versionUpdates == null) {
			return null;
		}

		return versionUpdates.get(version);
	}

	public List<FileUpdate> getFileUpdates() {
		List<FileUpdate> ret = new LinkedList<FileUpdate>();

		for (FileHistoryPart h : updates.values()) {
			ret.addAll(h.getAllValues());
		}

		return ret;
	}

	public List<FileHistoryPart> getHistories() {
		List<FileHistoryPart> histories = new ArrayList<FileHistoryPart>();

		for (Long fileId : getFileIds()) {
			histories.add(updates.get(fileId));
		}

		return histories;
	}

	public Set<Long> getFileIds() {
		return updates.keySet();
	}

	public void setVersions(List<CloneFile> versions) {
		this.versions = versions;
	}
}
