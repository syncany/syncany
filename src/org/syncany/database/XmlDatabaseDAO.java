package org.syncany.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.syncany.chunk.Transformer;
import org.syncany.database.ChunkEntry.ChunkEntryId;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.VectorClock.VectorClockComparison;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XmlDatabaseDAO implements DatabaseDAO {
	private static final Logger logger = Logger.getLogger(XmlDatabaseDAO.class.getSimpleName());
	private static final int XML_FORMAT_VERSION = 1;

	private Transformer transformer;
	
	public XmlDatabaseDAO() {
		this(null);
	}
	
	public XmlDatabaseDAO(Transformer transformer) {
		this.transformer = transformer;
	}
	
	@Override
	public void save(Database db, File destinationFile) throws IOException {
		save(db, null, null, destinationFile);
	}

	@Override
	public void save(Database db, DatabaseVersion versionFrom, DatabaseVersion versionTo, File destinationFile) throws IOException {
		PrintWriter out;
		
		if (transformer == null) {
			out = new PrintWriter(new FileWriter(destinationFile));
		}
		else {
			out = new PrintWriter(transformer.createOutputStream(new FileOutputStream(destinationFile)));
		}
		
		out.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		out.print("<database version=\""+XML_FORMAT_VERSION+"\">\n");
		out.print("\t<databaseVersions>\n");
		
		for (DatabaseVersion databaseVersion : db.getDatabaseVersions()) {
			boolean databaseVersionInSaveRange = databaseVersionInRange(databaseVersion, versionFrom, versionTo);

			if (!databaseVersionInSaveRange) {				
				continue;
			}		
			
			// Database version 
			out.print("\t\t<databaseVersion>\n");
		
			// Header
			if (databaseVersion.getTimestamp() == null || databaseVersion.getClient() == null
					|| databaseVersion.getVectorClock() == null || databaseVersion.getVectorClock().isEmpty()) {

				logger.log(Level.SEVERE, "Cannot write database version. Header fields must be filled: "+databaseVersion.getHeader());
				throw new IOException("Cannot write database version. Header fields must be filled: "+databaseVersion.getHeader());
			}
			
			out.print("\t\t\t<header>\n");	
			out.print("\t\t\t\t<time value=\""+databaseVersion.getTimestamp().getTime()+"\" />\n");
			out.print("\t\t\t\t<client name=\""+databaseVersion.getClient()+"\" />\n");			
			if (databaseVersion.getPreviousClient() != null && !"".equals(databaseVersion.getPreviousClient())) {
				out.print("\t\t\t\t<previousClient name=\""+databaseVersion.getPreviousClient()+"\" />\n");
			}			
			out.print("\t\t\t\t<vectorClock>\n");			
			VectorClock vectorClock = databaseVersion.getVectorClock();			
			for (Map.Entry<String, Long> vectorClockEntry : vectorClock.entrySet()) {
				out.print("\t\t\t\t\t<client name=\""+vectorClockEntry.getKey()+"\" value=\""+vectorClockEntry.getValue()+"\" />\n");
			}
			out.write("\t\t\t\t</vectorClock>\n");			
			out.write("\t\t\t</header>\n");				
			
			// Chunks
			Collection<ChunkEntry> chunks = databaseVersion.getChunks();
			
			if (chunks.size() > 0) {
				out.print("\t\t\t<chunks>\n");
				for (ChunkEntry chunk : chunks) {
					out.print("\t\t\t\t<chunk checksum=\""+StringUtil.toHex(chunk.getChecksum())+"\" size=\""+chunk.getSize()+"\" />\n");
				}			
				out.print("\t\t\t</chunks>\n");
			}
			
			// Multichunks
			Collection<MultiChunkEntry> multiChunks = databaseVersion.getMultiChunks();

			if (multiChunks.size() > 0) {
				out.print("\t\t\t<multiChunks>\n");
				for (MultiChunkEntry multiChunk : multiChunks) {
					out.print("\t\t\t\t<multiChunk id=\""+StringUtil.toHex(multiChunk.getId())+"\">\n");
					out.print("\t\t\t\t\t<chunkRefs>\n");
					Collection<ChunkEntryId> multiChunkChunks = multiChunk.getChunks();
					for (ChunkEntryId chunkChecksum : multiChunkChunks) {
						out.print("\t\t\t\t\t\t<chunkRef ref=\""+StringUtil.toHex(chunkChecksum.getArray())+"\" />\n");
					}			
					out.print("\t\t\t\t\t</chunkRefs>\n");				
					out.print("\t\t\t\t</multiChunk>\n");				
				}			
				out.print("\t\t\t</multiChunks>\n");
			}
			
			// File contents
			Collection<FileContent> fileContents = databaseVersion.getFileContents();

			if (fileContents.size() > 0) {
				out.print("\t\t\t<fileContents>\n");
				for (FileContent fileContent : fileContents) {
					out.print("\t\t\t\t<fileContent checksum=\""+StringUtil.toHex(fileContent.getChecksum())+"\" size=\""+fileContent.getSize()+"\">\n");
					out.print("\t\t\t\t\t<chunkRefs>\n");
					Collection<ChunkEntryId> fileContentChunkChunks = fileContent.getChunks();
					for (ChunkEntryId chunkChecksum : fileContentChunkChunks) {
						out.print("\t\t\t\t\t\t<chunkRef ref=\""+StringUtil.toHex(chunkChecksum.getArray())+"\" />\n");
					}			
					out.print("\t\t\t\t\t</chunkRefs>\n");				
					out.print("\t\t\t\t</fileContent>\n");				
				}						
				out.print("\t\t\t</fileContents>\n");
			}
			
			// File histories
			out.print("\t\t\t<fileHistories>\n");
			Collection<PartialFileHistory> fileHistories = databaseVersion.getFileHistories();
			for (PartialFileHistory fileHistory : fileHistories) {
				out.print("\t\t\t\t<fileHistory id=\""+fileHistory.getFileId()+"\">\n");
				out.print("\t\t\t\t\t<fileVersions>\n");
				Collection<FileVersion> fileVersions = fileHistory.getFileVersions().values();
				for (FileVersion fileVersion : fileVersions) {
					if (fileVersion.getVersion() == null || fileVersion.getType() == null || fileVersion.getPath() == null 
							|| fileVersion.getStatus() == null || fileVersion.getSize() == null || fileVersion.getLastModified() == null) {
						
						throw new IOException("Unable to write file version, because one or many mandatory fields are null (version, type, path, name, status, size, last modified): "+fileVersion);
					}
					
					if (fileVersion.getType() == FileType.SYMLINK && fileVersion.getLinkTarget() == null) {
						throw new IOException("Unable to write file version: All symlinks must have a target.");
					}
					
					out.print("\t\t\t\t\t\t<fileVersion");
					out.print(" version=\""+fileVersion.getVersion()+"\"");
					out.print(" type=\""+fileVersion.getType()+"\"");
					out.print(" status=\""+fileVersion.getStatus()+"\"");					
					out.print(" path=\""+FileUtil.toDatabaseFilePath(fileVersion.getPath())+"\"");
					out.print(" size=\""+fileVersion.getSize()+"\"");
					out.print(" lastModified=\""+fileVersion.getLastModified().getTime()+"\"");
					
					if (fileVersion.getLinkTarget() != null) {
						out.print(" linkTarget=\""+FileUtil.toDatabaseFilePath(fileVersion.getLinkTarget())+"\"");
					}

					if (fileVersion.getCreatedBy() != null) {
						out.print(" createdBy=\""+fileVersion.getCreatedBy()+"\"");
					}
					
					if (fileVersion.getUpdated() != null) {
						out.print(" updated=\""+fileVersion.getUpdated().getTime()+"\"");
					}
					
					if (fileVersion.getChecksum() != null) {
						out.print(" checksum=\""+StringUtil.toHex(fileVersion.getChecksum())+"\"");
					}
					
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
	
	private boolean vectorClockInRange(VectorClock vectorClock, VectorClock vectorClockRangeFrom, VectorClock vectorClockRangeTo) {
		// Determine if: versionFrom < databaseVersion
		boolean greaterOrEqualToVersionFrom = false;

		if (vectorClockRangeFrom == null) {
			greaterOrEqualToVersionFrom = true;
		}
		else {
			VectorClockComparison comparison = VectorClock.compare(vectorClockRangeFrom, vectorClock);
			
			if (comparison == VectorClockComparison.EQUAL || comparison == VectorClockComparison.SMALLER) {
				greaterOrEqualToVersionFrom = true;
			}				
		}
		
		// Determine if: databaseVersion < versionTo
		boolean lowerOrEqualToVersionTo = false;

		if (vectorClockRangeTo == null) {
			lowerOrEqualToVersionTo = true;
		}
		else {
			VectorClockComparison comparison = VectorClock.compare(vectorClock, vectorClockRangeTo);
			
			if (comparison == VectorClockComparison.EQUAL || comparison == VectorClockComparison.SMALLER) {
				lowerOrEqualToVersionTo = true;
			}				
		}

		return greaterOrEqualToVersionFrom && lowerOrEqualToVersionTo;		
	}
	
	private boolean databaseVersionInRange(DatabaseVersion databaseVersion, DatabaseVersion databaseVersionFrom, DatabaseVersion databaseVersionTo) {
		VectorClock vectorClock = databaseVersion.getVectorClock();
		VectorClock vectorClockRangeFrom = (databaseVersionFrom != null) ? databaseVersionFrom.getVectorClock() : null;
		VectorClock vectorClockRangeTo = (databaseVersionTo != null) ? databaseVersionTo.getVectorClock() : null;
		
		return vectorClockInRange(vectorClock, vectorClockRangeFrom, vectorClockRangeTo);
	}	

	@Override
	public void load(Database db, File databaseFile) throws IOException {
        load(db, databaseFile, null, null);
	}
	
	@Override
	public void load(Database db, File databaseFile, VectorClock fromVersion, VectorClock toVersion) throws IOException {
        InputStream is;
        
		if (transformer == null) {
			is = new FileInputStream(databaseFile);
		}
		else {
			is = transformer.createInputStream(new FileInputStream(databaseFile));
		}
        
        try {
			logger.log(Level.INFO, "- Loading database from "+databaseFile+" ...");

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			
			saxParser.parse(is, new DatabaseXmlHandler(db, fromVersion, toVersion));
        }
        catch (Exception e) {
        	throw new IOException(e);
        } 
	}	
	
	public class DatabaseXmlHandler extends DefaultHandler {
		private Database database;
		private VectorClock versionFrom;
		private VectorClock versionTo;

		private String elementPath;
		private DatabaseVersion databaseVersion;
		private VectorClock vectorClock;
		private boolean vectorClockInLoadRange;
		private FileContent fileContent;
		private MultiChunkEntry multiChunk;
		private PartialFileHistory fileHistory;
		
		public DatabaseXmlHandler(Database database, VectorClock fromVersion, VectorClock toVersion) {
			this.elementPath = "";
			this.database = database;
			this.versionFrom = fromVersion;
			this.versionTo = toVersion;
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			elementPath += "/"+qName;
			
			//System.out.println(elementPath+" (start) ");
			
			if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion")) {				
				databaseVersion = new DatabaseVersion();				
			}			
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/header/time")) {
				Date timeValue = new Date(Long.parseLong(attributes.getValue("value")));
				databaseVersion.setTimestamp(timeValue);
			}
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/header/client")) {
				String clientName = attributes.getValue("name");
				databaseVersion.setClient(clientName);
			}
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/header/previousClient")) {
				String previousClientName = attributes.getValue("name");
				databaseVersion.setPreviousClient(previousClientName);
			}
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/header/vectorClock")) {
				vectorClock = new VectorClock();
			}
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/header/vectorClock/client")) {
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
				fileContent.setSize(size);							
			}
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/fileContents/fileContent/chunkRefs/chunkRef")) {
				String chunkChecksumStr = attributes.getValue("ref");
				byte[] chunkChecksum = StringUtil.fromHex(chunkChecksumStr);

				fileContent.addChunk(new ChunkEntryId(chunkChecksum));
			}
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/multiChunks/multiChunk")) {
				String multChunkIdStr = attributes.getValue("id");
				byte[] multiChunkId = StringUtil.fromHex(multChunkIdStr);
				
				if (multiChunkId == null) {
					throw new SAXException("Cannot read ID from multichunk " + multChunkIdStr);
				}
				
				multiChunk = new MultiChunkEntry(multiChunkId);					
			}			
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/multiChunks/multiChunk/chunkRefs/chunkRef")) {
				String chunkChecksumStr = attributes.getValue("ref");
				byte[] chunkChecksum = StringUtil.fromHex(chunkChecksumStr);

				multiChunk.addChunk(new ChunkEntryId(chunkChecksum));
			}
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/fileHistories/fileHistory")) {
				String fileHistoryIdStr = attributes.getValue("id");
				Long fileHistoryId = Long.parseLong(fileHistoryIdStr);
				
				fileHistory = new PartialFileHistory();
				fileHistory.setFileId(fileHistoryId);
			}	
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/fileHistories/fileHistory/fileVersions/fileVersion")) {
				String fileVersionStr = attributes.getValue("version");
				String path = attributes.getValue("path");
				String sizeStr = attributes.getValue("size");
				String typeStr = attributes.getValue("type");
				String statusStr = attributes.getValue("status");
				String lastModifiedStr = attributes.getValue("lastModified");
				String updatedStr = attributes.getValue("updated");
				String createdBy = attributes.getValue("createdBy");
				String checksumStr = attributes.getValue("checksum");
				String linkTarget = attributes.getValue("linkTarget");
				
				if (fileVersionStr == null || path == null || typeStr == null || statusStr == null || sizeStr == null || lastModifiedStr == null) {
					throw new SAXException("FileVersion: Attributes missing: version, path, type, status, size and last modified are mandatory");
				}
				
				FileVersion fileVersion = new FileVersion();
				 
				fileVersion.setVersion(Long.parseLong(fileVersionStr));
				fileVersion.setPath(path);
				fileVersion.setType(FileType.valueOf(typeStr));
				fileVersion.setStatus(FileStatus.valueOf(statusStr));
				fileVersion.setSize(Long.parseLong(sizeStr));				
				fileVersion.setLastModified(new Date(Long.parseLong(lastModifiedStr)));
				
				if (updatedStr != null) {
					fileVersion.setUpdated(new Date(Long.parseLong(updatedStr)));
				}
				
				if (createdBy != null) {
					fileVersion.setCreatedBy(createdBy);
				}
				
				if (checksumStr != null) {
					fileVersion.setChecksum(StringUtil.fromHex(checksumStr));							
				}
				
				if (linkTarget != null) {
					fileVersion.setLinkTarget(linkTarget);
				}

				fileHistory.addFileVersion(fileVersion);							
			}
			else {
				//System.out.println("NO MATCH");
			}
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			//System.out.println(elementPath+" (end ) ");
			
			if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion")) {
				if (vectorClockInLoadRange) {
					database.addDatabaseVersion(databaseVersion);
					logger.log(Level.INFO, "   + Added database version "+databaseVersion.getHeader());
				}
				else {
					logger.log(Level.INFO, "   + Ignoring database version "+databaseVersion.getHeader()+", not in load range: "+versionFrom+" - "+versionTo);
				}

				databaseVersion = null;
			}	
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/header/vectorClock")) {				
				vectorClockInLoadRange = vectorClockInRange(vectorClock, versionFrom, versionTo);
				
				databaseVersion.setVectorClock(vectorClock);
				vectorClock = null;
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
				//System.out.println("NO MATCH");
			}
			
			elementPath = elementPath.substring(0, elementPath.lastIndexOf("/"));					
		}
				
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			/*String textContent = new String(ch, start, length);
			
			if (elementPath.equalsIgnoreCase("/database/databaseVersions/...")) {
				...
			}*/			
		}		
	}
}
