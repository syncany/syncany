package org.syncany.experimental.db.dao;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.syncany.experimental.db.Database;
import org.syncany.experimental.db.FileContent;
import org.syncany.experimental.db.FileHistory;
import org.syncany.experimental.db.FileVersion;

public class FileHistoryDAO {
	public void writeFileHistory(FileHistory fileHistory, DataOutputStream dos) throws IOException {
		// File history
		dos.writeLong(fileHistory.getFileId());  
		
		// And its versions
		Collection<FileVersion> versions = fileHistory.getFileVersions().values();
		
		if (versions.isEmpty()) {
			dos.writeInt(0); // count						
		}
		else {
			for (FileVersion fileVersion : versions) {
				writeFileVersion(fileVersion, dos);
			}
		}		
	}
	
	public FileHistory readFileHistory(Database db, DataInputStream dis) throws IOException {
		// File history
		FileHistory fileHistory = new FileHistory();
		fileHistory.setFileId(dis.readLong());

		// And its versions
		int fileVersionCount = dis.readInt();
		
		for (int i=0; i<fileVersionCount; i++) {
			FileVersion fileVersion = readFileVersion(db, fileHistory, dis);
			fileHistory.addFileVersion(fileVersion);
		}
		
		return fileHistory;
	}	

	private void writeFileVersion(FileVersion fileVersion, DataOutput out) throws IOException {
		out.writeLong(fileVersion.getVersion());

		if (fileVersion.getContent() == null) {
			out.writeByte(0x00);
		} else {
			out.writeByte(0x01);
			out.writeByte(fileVersion.getContent().getChecksum().length);
			out.write(fileVersion.getContent().getChecksum());
		}

		out.writeShort(fileVersion.getPath().length());
		out.writeBytes(fileVersion.getPath());

		out.writeShort(fileVersion.getName().length());
		out.writeBytes(fileVersion.getName());
	}
	
	private FileVersion readFileVersion(Database db, FileHistory fileHistory, DataInputStream dis) throws IOException {
		FileVersion fileVersion = new FileVersion();
				
		// Version
		fileVersion.setVersion(dis.readLong());

		// Content
		byte hasContent = dis.readByte();

		if ((hasContent & 0xff) == 0x01) {
			byte fileContentChecksumLength = dis.readByte();
			byte[] fileContentChecksum = new byte[fileContentChecksumLength];
			dis.readFully(fileContentChecksum);

			FileContent fileContent = db.getContent(fileContentChecksum);
			
			if (fileContent == null) {
				throw new IOException("File content with checksum "
						+ Arrays.toString(fileContentChecksum) + " does not exist.");
			}
			
			fileVersion.setContent(fileContent);
		}

		// Other properties
		short pathBytesLength = dis.readShort();
		byte[] pathBytes = new byte[pathBytesLength];
		dis.readFully(pathBytes);
		fileVersion.setPath(new String(pathBytes));

		short nameBytesLength = dis.readShort();
		byte[] nameBytes = new byte[nameBytesLength];
		dis.readFully(nameBytes);
		fileVersion.setName(new String(nameBytes));

		return fileVersion;
	}
	
}
