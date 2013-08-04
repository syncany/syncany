package org.syncany.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.syncany.database.VectorClock.VectorClockComparison;
import org.syncany.operations.DatabaseFile;
import org.syncany.util.StringUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class DatabaseXmlDAO implements DatabaseDAO {

	@Override
	public void save(Database db, File destinationFile) throws IOException {
		save(db, null, null, destinationFile);
	}

	@Override
	public void save(Database db, DatabaseVersion versionFrom, DatabaseVersion versionTo, File destinationFile) throws IOException {
		FileWriter fileWriter = new FileWriter(destinationFile);
		PrintWriter out = new PrintWriter(fileWriter);
		
		out.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		out.print("<database>\n");
		out.print("\t<version>1</version>\n");
		out.print("\t<databaseVersions>\n");
		
		for (DatabaseVersion databaseVersion : db.getDatabaseVersions()) {
			if ((versionFrom != null && VectorClock.compare(versionFrom.getVectorClock(), databaseVersion.getVectorClock()) == VectorClockComparison.SMALLER)
					|| (versionTo != null && VectorClock.compare(databaseVersion.getVectorClock(), versionTo.getVectorClock()) == VectorClockComparison.GREATER)) {
				
				continue;
			}		
			
			out.print("\t\t<databaseVersion>\n");
			
			// Local timestamp
			out.print("\t\t\t<time>"+databaseVersion.getTimestamp().getTime()+"</time>\n");
			
			// Vector clock
			out.print("\t\t\t<vectorClock>\n");			
			VectorClock vectorClock = databaseVersion.getVectorClock();			
			for (Map.Entry<String, Long> vectorClockEntry : vectorClock.entrySet()) {
				out.print("\t\t\t\t<client name=\""+vectorClockEntry.getKey()+"\" value=\""+vectorClockEntry.getValue()+"\" />\n");
			}
			out.write("\t\t\t</vectorClock>\n");
			
			// Chunks
			out.print("\t\t\t<chunks>\n");
			Collection<ChunkEntry> chunks = databaseVersion.getChunks();
			for (ChunkEntry chunk : chunks) {
				out.print("\t\t\t\t<chunk checksum=\""+StringUtil.toHex(chunk.getChecksum())+"\" size=\""+chunk.getSize()+"\" />\n");
			}			
			out.print("\t\t\t</chunks>\n");

			// Multichunks
			out.print("\t\t\t<multiChunks>\n");
			Collection<MultiChunkEntry> multiChunks = databaseVersion.getMultiChunks();
			for (MultiChunkEntry multiChunk : multiChunks) {
				out.print("\t\t\t\t<multiChunk id=\""+StringUtil.toHex(multiChunk.getId())+"\">\n");
				out.print("\t\t\t\t\t<chunkRefs>\n");
				Collection<ChunkEntry> multiChunkChunks = multiChunk.getChunks();
				for (ChunkEntry chunk : multiChunkChunks) {
					out.print("\t\t\t\t\t\t<chunkRef ref=\""+StringUtil.toHex(chunk.getChecksum())+"\" />\n");
				}			
				out.print("\t\t\t\t\t</chunkRefs>\n");				
				out.print("\t\t\t\t</multiChunk>\n");				
			}			
			out.print("\t\t\t</multiChunks>\n");
			
			// File contents
			out.print("\t\t\t<fileContents>\n");
			Collection<FileContent> fileContents = databaseVersion.getFileContents();
			for (FileContent fileContent : fileContents) {
				out.print("\t\t\t\t<fileContent checksum=\""+StringUtil.toHex(fileContent.getChecksum())+"\" size=\""+fileContent.getContentSize()+"\">\n");
				out.print("\t\t\t\t\t<chunkRefs>\n");
				Collection<ChunkEntry> fileContentChunkChunks = fileContent.getChunks();
				for (ChunkEntry chunk : fileContentChunkChunks) {
					out.print("\t\t\t\t\t\t<chunkRef ref=\""+StringUtil.toHex(chunk.getChecksum())+"\" />\n");
				}			
				out.print("\t\t\t\t\t</chunkRefs>\n");				
				out.print("\t\t\t\t</fileContent>\n");				
			}						
			out.print("\t\t\t</fileContents>\n");

			// File histories
			out.print("\t\t\t<fileHistories>\n");
			Collection<PartialFileHistory> fileHistories = databaseVersion.getFileHistories();
			for (PartialFileHistory fileHistory : fileHistories) {
				out.print("\t\t\t\t<fileHistory id=\""+fileHistory.getFileId()+"\">\n");
				out.print("\t\t\t\t\t<fileVersions>\n");
				Collection<FileVersion> fileVersions = fileHistory.getFileVersions().values();
				for (FileVersion fileVersion : fileVersions) {
					out.print("\t\t\t\t\t\t<fileVersion");
						out.print(" version=\""+fileVersion.getVersion()+"\"");
						
						if (fileVersion.getCreatedBy() != null) {
							out.print(" createdBy=\""+fileVersion.getCreatedBy()+"\"");
						}
						
						if (fileVersion.getLastModified() != null) {
							out.print(" lastModified=\""+fileVersion.getLastModified().getTime()+"\"");
						}
						
						if (fileVersion.getContent() != null) {
							out.print(" fileContentRef=\""+StringUtil.toHex(fileVersion.getContent().getChecksum())+"\"");
						}
						
						out.print(" path=\""+fileVersion.getPath()+"\"");
						out.print(" name=\""+fileVersion.getName()+"\"");
					out.print(" />\n");
				}			
				out.print("\t\t\t\t\t</fileVersions>\n");				
				out.print("\t\t\t\t</fileHistory>\n");				
			}						
			out.print("\t\t\t</fileHistories>\n");
			
			// End of database version
			out.print("\t\t</databaseVersion>\n");
		}
		
		out.print("\t</databaseVersions>\n");
		out.print("</database>\n");
		
		out.flush();
		out.close();
	}
	
	public void load(Database db, DatabaseFile databaseFile) throws IOException {
        InputStream is = new FileInputStream(databaseFile.getFile());
        
        try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			
			saxParser.parse(is, new DatabaseXmlHandler(db));
        }
        catch (Exception e) {
        	throw new IOException(e);
        } 
	}
	
	public static class DatabaseXmlHandler extends DefaultHandler {
		private String elementPath;
		private Database database;
		private DatabaseVersion databaseVersion;
		private VectorClock vectorClock;
		private FileContent fileContent;
		private MultiChunkEntry multiChunk;
		private PartialFileHistory fileHistory;
		
		public DatabaseXmlHandler(Database database) {
			this.elementPath = "";
			this.database = database;
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			elementPath += "/"+qName;
			
			System.out.println(elementPath+" (start) ");
			
			if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion")) {
				databaseVersion = new DatabaseVersion();
			}			
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/vectorClock")) {
				vectorClock = new VectorClock();
			}
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/vectorClock/client")) {
				String clientName = attributes.getValue("name");
				Long clientValue = Long.parseLong(attributes.getValue("value"));
				
				vectorClock.setClock(clientName, clientValue);
			}			
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/chunks/chunk")) {
				String chunkChecksumStr = attributes.getValue("checksum");
				byte[] chunkChecksum = StringUtil.fromHex(chunkChecksumStr);
				int chunkSize = Integer.parseInt(attributes.getValue("size"));
				
				ChunkEntry chunkEntry = new ChunkEntry(chunkChecksum, chunkSize);
				databaseVersion.addChunk(chunkEntry);
			}
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/fileContents/fileContent")) {
				String checksumStr = attributes.getValue("checksum");
				byte[] checksum = StringUtil.fromHex(checksumStr);
				int size = Integer.parseInt(attributes.getValue("size"));

				fileContent = new FileContent();
				fileContent.setChecksum(checksum);
				fileContent.setContentSize(size);							
			}
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/fileContents/fileContent/chunkRef")) {
				String chunkChecksumStr = attributes.getValue("ref");
				byte[] chunkChecksum = StringUtil.fromHex(chunkChecksumStr);
						
				ChunkEntry chunkEntry = database.getChunk(chunkChecksum);

				if (chunkEntry == null) {
					chunkEntry = databaseVersion.getChunk(chunkChecksum);
					
					if (chunkEntry == null) {
						throw new SAXException("Chunk with checksum " + chunkChecksumStr + " does not exist.");
					}
				}  

				fileContent.addChunk(chunkEntry);
			}
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/multiChunks/multiChunk")) {
				String multChunkIdStr = attributes.getValue("id");
				byte[] multiChunkId = StringUtil.fromHex(multChunkIdStr);
				
				if (multiChunkId == null) {
					throw new SAXException("Cannot read ID from multichunk " + multChunkIdStr);
				}
				
				multiChunk = new MultiChunkEntry(multiChunkId);	
				System.out.println("multichunk = "+multiChunk);
			}			
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/multiChunks/multiChunk/chunkRef")) {
				String chunkChecksumStr = attributes.getValue("ref");
				byte[] chunkChecksum = StringUtil.fromHex(chunkChecksumStr);
						
				ChunkEntry chunkEntry = database.getChunk(chunkChecksum);

				if (chunkEntry == null) {
					chunkEntry = databaseVersion.getChunk(chunkChecksum);
					
					if (chunkEntry == null) {
						throw new SAXException("Chunk with checksum " + chunkChecksumStr + " does not exist.");
					}
				}

				multiChunk.addChunk(chunkEntry);
			}
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/fileHistories/fileHistory")) {
				String fileHistoryIdStr = attributes.getValue("id");
				Long fileHistoryId = Long.parseLong(fileHistoryIdStr);
				
				fileHistory = new PartialFileHistory();
				fileHistory.setFileId(fileHistoryId);
			}	
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/fileHistories/fileHistory/fileVersions/fileVersion")) {
				String fileVersionStr = attributes.getValue("version");
				String lastModifiedStr = attributes.getValue("lastModifiedStr");				
				String createdBy = attributes.getValue("createdBy");
				String path = attributes.getValue("path");
				String name = attributes.getValue("name");
				String fileContentRefStr = attributes.getValue("fileContentRef");
				
				FileVersion fileVersion = new FileVersion();
				
				fileVersion.setFileId(fileHistory.getFileId());
				fileVersion.setVersion(Long.parseLong(fileVersionStr));
				
				if (lastModifiedStr != null) {
					fileVersion.setLastModified(new Date(Long.parseLong(lastModifiedStr)));
				}
				
				if (createdBy != null) {
					fileVersion.setCreatedBy(createdBy);
				}
				
				fileVersion.setPath(path);
				fileVersion.setName(name);

				// Ref. to content
				if (fileContentRefStr != null) {
					byte[] fileContentRef = StringUtil.fromHex(fileContentRefStr);
							
					FileContent aFileContent = database.getContent(fileContentRef);
	
					if (aFileContent == null) {
						aFileContent = databaseVersion.getFileContent(fileContentRef);
						
						if (aFileContent == null) {
							throw new SAXException("File content with checksum " + fileContentRefStr + " does not exist.");
						}
					}
				}

				fileHistory.addFileVersion(fileVersion);
			}
			else {
				System.out.println("NO MATCH");
			}
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			System.out.println(elementPath+" (end ) ");
			
			if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion")) {
				database.addDatabaseVersion(databaseVersion);
				databaseVersion = null;
			}	
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/fileContents/fileContent")) {
				databaseVersion.addFileContent(fileContent);
				fileContent = null;
			}	
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/multiChunks/multiChunk")) {
				databaseVersion.addMultiChunk(multiChunk);
				multiChunk = null;
			}	
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/fileHistories/fileHistory")) {
				databaseVersion.addFileHistory(fileHistory);
				fileHistory = null;
			}	
			else {
				System.out.println("NO MATCH");
			}
			
			elementPath = elementPath.substring(0, elementPath.lastIndexOf("/"));					
		}
				
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			System.out.println(elementPath+" (chars) : "+new String(ch, start, length));
			
			// Database version
			if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/time")) {
				databaseVersion.setTimestamp(new Date(Long.parseLong(new String(ch, start, length))));				
			}
			else {
				System.out.println("NO MATCH");
			}
		}		
	}
}
