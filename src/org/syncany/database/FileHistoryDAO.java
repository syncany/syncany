package org.syncany.database;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;


public class FileHistoryDAO {
	public void writeFileHistory(PartialFileHistory fileHistory, DataOutputStream dos) throws IOException {
		// File history
		dos.writeLong(fileHistory.getFileId());  
		
		// And its versions
		Collection<FileVersion> versions = fileHistory.getFileVersions().values();
		
		if (versions.isEmpty()) {
			dos.writeInt(0); // count						
		}
		else { 
			dos.writeInt(versions.size());
			
			for (FileVersion fileVersion : versions) {
				writeFileVersion(fileVersion, dos);
			}
		}		
	}
	
	public PartialFileHistory readFileHistory(Database db, DatabaseVersion dbv, DataInputStream dis) throws IOException {
		// File history
		PartialFileHistory fileHistory = new PartialFileHistory();
		
		long fileId = dis.readLong();
		fileHistory.setFileId(fileId);

		// And its versions
		int fileVersionCount = dis.readInt();
		
		for (int i=0; i<fileVersionCount; i++) {
			FileVersion fileVersion = readFileVersion(db, dbv, fileHistory, dis);
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
	
	private FileVersion readFileVersion(Database db, DatabaseVersion dbv, PartialFileHistory fileHistory, DataInputStream dis) throws IOException {
		FileVersion fileVersion = new FileVersion();
				
		// Version
		long version = dis.readLong();
		fileVersion.setVersion(version);
		
		// Content
		byte hasContent = dis.readByte();		
		
		if (hasContent == 0x01) {
			byte fileContentChecksumLength = dis.readByte();
			byte[] fileContentChecksum = new byte[fileContentChecksumLength];
			dis.readFully(fileContentChecksum);

			FileContent fileContent = db.getContent(fileContentChecksum);
			
			if (fileContent == null) {
				fileContent = dbv.getFileContent(fileContentChecksum);
				
				if (fileContent == null) {
					throw new IOException("File content with checksum " + Arrays.toString(fileContentChecksum) + " does not exist.");
				}
			}
			
			fileVersion.setContent(fileContent);
		}

		// Other properties
		short pathBytesLength = dis.readShort();
		byte[] pathBytes = new byte[pathBytesLength];
		dis.readFully(pathBytes);
		String path = new String(pathBytes);
		fileVersion.setPath(path);

		short nameBytesLength = dis.readShort();
		byte[] nameBytes = new byte[nameBytesLength];
		dis.readFully(nameBytes);
		String name = new String(nameBytes);
		fileVersion.setName(name);

		return fileVersion;
	}
	
}
