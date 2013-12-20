/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.syncany.chunk.Transformer;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.VectorClock.VectorClockComparison;
import org.syncany.util.FileUtil;
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
		try {
			PrintWriter out;
			
			if (transformer == null) {
				out = new PrintWriter(new OutputStreamWriter(
						new FileOutputStream(destinationFile), "UTF-8"));
			}
			else {
				out = new PrintWriter(new OutputStreamWriter(
						transformer.createOutputStream(new FileOutputStream(destinationFile)), "UTF-8"));
			}
			
			// Initialize XML writer
			IndentXmlStreamWriter xmlOut = new IndentXmlStreamWriter(out);
			
			xmlOut.writeStartDocument();
			
			xmlOut.writeStartElement("database");
			xmlOut.writeAttribute("version", XML_FORMAT_VERSION);
			 
			xmlOut.writeStartElement("databaseVersions");
			 			
			for (DatabaseVersion databaseVersion : db.getDatabaseVersions()) {
				boolean databaseVersionInSaveRange = databaseVersionInRange(databaseVersion, versionFrom, versionTo);

				if (!databaseVersionInSaveRange) {				
					continue;
				}						

				// Database version
				xmlOut.writeStartElement("databaseVersion");
				
				// Header, chunks, multichunks, file contents, and file histories
				writeDatabaseVersionHeader(xmlOut, databaseVersion);
				writeChunks(xmlOut, databaseVersion.getChunks());
				writeMultiChunks(xmlOut, databaseVersion.getMultiChunks());
				writeFileContents(xmlOut, databaseVersion.getFileContents());
				writeFileHistories(xmlOut, databaseVersion.getFileHistories());	
				
				xmlOut.writeEndElement(); // </databaserVersion>
			}
			
			xmlOut.writeEndElement(); // </databaseVersions>
			xmlOut.writeEndElement(); // </database>
			 
			xmlOut.writeEndDocument();
			
			xmlOut.flush();
			xmlOut.close();
			
			out.flush();
			out.close();
		}
		catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}			

	private void writeDatabaseVersionHeader(IndentXmlStreamWriter xmlOut, DatabaseVersion databaseVersion) throws IOException, XMLStreamException {
		if (databaseVersion.getTimestamp() == null || databaseVersion.getClient() == null
				|| databaseVersion.getVectorClock() == null || databaseVersion.getVectorClock().isEmpty()) {

			logger.log(Level.SEVERE, "Cannot write database version. Header fields must be filled: "+databaseVersion.getHeader());
			throw new IOException("Cannot write database version. Header fields must be filled: "+databaseVersion.getHeader());
		}
		
		xmlOut.writeStartElement("header");
		
		xmlOut.writeEmptyElement("time");
		xmlOut.writeAttribute("value", databaseVersion.getTimestamp().getTime());
		
		xmlOut.writeEmptyElement("client");
		xmlOut.writeAttribute("name", databaseVersion.getClient());
		
		xmlOut.writeStartElement("vectorClock");

		VectorClock vectorClock = databaseVersion.getVectorClock();			
		for (Map.Entry<String, Long> vectorClockEntry : vectorClock.entrySet()) {
			xmlOut.writeEmptyElement("client");
			xmlOut.writeAttribute("name", vectorClockEntry.getKey());
			xmlOut.writeAttribute("value", vectorClockEntry.getValue());
		}
		
		xmlOut.writeEndElement(); // </vectorClock>
		xmlOut.writeEndElement(); // </header>	
	}
	
	private void writeChunks(IndentXmlStreamWriter xmlOut, Collection<ChunkEntry> chunks) throws XMLStreamException {
		if (chunks.size() > 0) {
			xmlOut.writeStartElement("chunks");
								
			for (ChunkEntry chunk : chunks) {
				xmlOut.writeEmptyElement("chunk");
				xmlOut.writeAttribute("checksum", chunk.getChecksum().toString());
				xmlOut.writeAttribute("size", chunk.getSize());
			}
			
			xmlOut.writeEndElement(); // </chunks>
		}		
	}
	
	private void writeMultiChunks(IndentXmlStreamWriter xmlOut, Collection<MultiChunkEntry> multiChunks) throws XMLStreamException {
		if (multiChunks.size() > 0) {
			xmlOut.writeStartElement("multiChunks");
			
			for (MultiChunkEntry multiChunk : multiChunks) {
				xmlOut.writeStartElement("multiChunk");
				xmlOut.writeAttribute("id", multiChunk.getId().toString());
			
				xmlOut.writeStartElement("chunkRefs");
				
				Collection<ChunkChecksum> multiChunkChunks = multiChunk.getChunks();
				for (ChunkChecksum chunkChecksum : multiChunkChunks) {
					xmlOut.writeEmptyElement("chunkRef");
					xmlOut.writeAttribute("ref", chunkChecksum.toString());
				}			
				
				xmlOut.writeEndElement(); // </chunkRefs>
				xmlOut.writeEndElement(); // </multiChunk>			
			}			
			
			xmlOut.writeEndElement(); // </multiChunks>
		}
	}
	
	private void writeFileContents(IndentXmlStreamWriter xmlOut, Collection<FileContent> fileContents) throws XMLStreamException {
		if (fileContents.size() > 0) {
			xmlOut.writeStartElement("fileContents");
			
			for (FileContent fileContent : fileContents) {
				xmlOut.writeStartElement("fileContent");
				xmlOut.writeAttribute("checksum", fileContent.getChecksum().toString());
				xmlOut.writeAttribute("size", fileContent.getSize());
				
				xmlOut.writeStartElement("chunkRefs");
				
				Collection<ChunkChecksum> fileContentChunkChunks = fileContent.getChunks();
				for (ChunkChecksum chunkChecksum : fileContentChunkChunks) {
					xmlOut.writeEmptyElement("chunkRef");
					xmlOut.writeAttribute("ref", chunkChecksum.toString());
				}			

				xmlOut.writeEndElement(); // </chunkRefs>
				xmlOut.writeEndElement(); // </fileContent>			
			}	
			
			xmlOut.writeEndElement(); // </fileContents>					
		}		
	}
	
	private void writeFileHistories(IndentXmlStreamWriter xmlOut, Collection<PartialFileHistory> fileHistories) throws XMLStreamException, IOException {
		xmlOut.writeStartElement("fileHistories");
		
		for (PartialFileHistory fileHistory : fileHistories) {
			xmlOut.writeStartElement("fileHistory");
			xmlOut.writeAttribute("id", fileHistory.getFileId().toString());
			
			xmlOut.writeStartElement("fileVersions");
			
			Collection<FileVersion> fileVersions = fileHistory.getFileVersions().values();
			for (FileVersion fileVersion : fileVersions) {
				if (fileVersion.getVersion() == null || fileVersion.getType() == null || fileVersion.getPath() == null 
						|| fileVersion.getStatus() == null || fileVersion.getSize() == null || fileVersion.getLastModified() == null) {
					
					throw new IOException("Unable to write file version, because one or many mandatory fields are null (version, type, path, name, status, size, last modified): "+fileVersion);
				}
				
				if (fileVersion.getType() == FileType.SYMLINK && fileVersion.getLinkTarget() == null) {
					throw new IOException("Unable to write file version: All symlinks must have a target.");
				}
				
				xmlOut.writeEmptyElement("fileVersion");
				xmlOut.writeAttribute("version", fileVersion.getVersion());
				xmlOut.writeAttribute("type", fileVersion.getType().toString());
				xmlOut.writeAttribute("status", fileVersion.getStatus().toString());
				xmlOut.writeAttribute("path", fileVersion.getPath());
				xmlOut.writeAttribute("size", fileVersion.getSize());
				xmlOut.writeAttribute("lastModified", fileVersion.getLastModified().getTime());						
				
				if (fileVersion.getLinkTarget() != null) {
					xmlOut.writeAttribute("linkTarget", fileVersion.getLinkTarget());
				}

				if (fileVersion.getCreatedBy() != null) {
					xmlOut.writeAttribute("createdBy", fileVersion.getCreatedBy());
				}
				
				if (fileVersion.getUpdated() != null) {
					xmlOut.writeAttribute("updated", fileVersion.getUpdated().getTime());
				}
				
				if (fileVersion.getChecksum() != null) {
					xmlOut.writeAttribute("checksum", fileVersion.getChecksum().toString());
				}
				
				if (fileVersion.getDosAttributes() != null) {
					xmlOut.writeAttribute("dosattrs", fileVersion.getDosAttributes());
				}
				
				if (fileVersion.getPosixPermissions() != null) {
					xmlOut.writeAttribute("posixperms", fileVersion.getPosixPermissions());
				}
			}
			
			xmlOut.writeEndElement(); // </fileVersions>
			xmlOut.writeEndElement(); // </fileHistory>	
		}						
		
		xmlOut.writeEndElement(); // </fileHistories>		
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
        load(db, databaseFile, false);
	}
	
	@Override
	public void load(Database db, File databaseFile, boolean headersOnly) throws IOException {
        load(db, databaseFile, null, null, headersOnly);
	}
	
	@Override
	public void load(Database db, File databaseFile, VectorClock fromVersion, VectorClock toVersion) throws IOException {
		load(db, databaseFile, fromVersion, toVersion, false);
	}
	
	@Override
	public void load(Database db, File databaseFile, VectorClock fromVersion, VectorClock toVersion, boolean headersOnly) throws IOException {
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
			
			saxParser.parse(is, new DatabaseXmlHandler(db, fromVersion, toVersion, headersOnly));
        }
        catch (Exception e) {
        	throw new IOException(e);
        } 
	}	
	
	public static class IndentXmlStreamWriter {
		private int indent;
		private XMLStreamWriter out;
		
		public IndentXmlStreamWriter(Writer out) throws XMLStreamException {
			XMLOutputFactory factory = XMLOutputFactory.newInstance();
			
			this.indent = 0;
			this.out = factory.createXMLStreamWriter(out);
		}
		
		public void writeStartDocument() throws XMLStreamException {
			out.writeStartDocument();
		}

		public void writeStartElement(String s) throws XMLStreamException {
			writeNewLineAndIndent(indent++);
			out.writeStartElement(s);	
		}
		
		public void writeEmptyElement(String s) throws XMLStreamException {
			writeNewLineAndIndent(indent);			
			out.writeEmptyElement(s);	
		}
		
		public void writeAttribute(String name, String value) throws XMLStreamException {
			out.writeAttribute(name, value);
		}
		
		public void writeAttribute(String name, int value) throws XMLStreamException {
			out.writeAttribute(name, Integer.toString(value));
		}
		
		public void writeAttribute(String name, long value) throws XMLStreamException {
			out.writeAttribute(name, Long.toString(value));
		}
		
		public void writeEndElement() throws XMLStreamException {			
			writeNewLineAndIndent(--indent);
			out.writeEndElement();			
		}
		
		public void writeEndDocument() throws XMLStreamException {
			out.writeEndDocument();
		}
		
		public void close() throws XMLStreamException {
			out.close();
		}
		
		public void flush() throws XMLStreamException {
			out.flush();
		}	
		
		private void writeNewLineAndIndent(int indent) throws XMLStreamException {
			out.writeCharacters("\n");			
			
			for (int i=0; i<indent; i++) {
				out.writeCharacters("\t");
			}
		}
	}
	
	public class DatabaseXmlHandler extends DefaultHandler {
		private Database database;
		private VectorClock versionFrom;
		private VectorClock versionTo;
		private boolean headersOnly;

		private String elementPath;
		private DatabaseVersion databaseVersion;
		private VectorClock vectorClock;
		private boolean vectorClockInLoadRange;
		private FileContent fileContent;
		private MultiChunkEntry multiChunk;
		private PartialFileHistory fileHistory;
		
		public DatabaseXmlHandler(Database database, VectorClock fromVersion, VectorClock toVersion, boolean headersOnly) {
			this.elementPath = "";
			this.database = database;
			this.versionFrom = fromVersion;
			this.versionTo = toVersion;
			this.headersOnly = headersOnly;
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
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/header/vectorClock")) {
				vectorClock = new VectorClock();
			}
			else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/header/vectorClock/client")) {
				String clientName = attributes.getValue("name");
				Long clientValue = Long.parseLong(attributes.getValue("value"));
				
				vectorClock.setClock(clientName, clientValue);
			}		
			else if (!headersOnly) {
				if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/chunks/chunk")) {
					String chunkChecksumStr = attributes.getValue("checksum");
					ChunkChecksum chunkChecksum = ChunkChecksum.parseChunkChecksum(chunkChecksumStr);
					int chunkSize = Integer.parseInt(attributes.getValue("size"));
					
					ChunkEntry chunkEntry = new ChunkEntry(chunkChecksum, chunkSize);
					databaseVersion.addChunk(chunkEntry);
				}
				else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/fileContents/fileContent")) {
					String checksumStr = attributes.getValue("checksum");
					long size = Long.parseLong(attributes.getValue("size"));
	
					fileContent = new FileContent();
					fileContent.setChecksum(FileChecksum.parseFileChecksum(checksumStr));
					fileContent.setSize(size);							
				}
				else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/fileContents/fileContent/chunkRefs/chunkRef")) {
					String chunkChecksumStr = attributes.getValue("ref");
	
					fileContent.addChunk(ChunkChecksum.parseChunkChecksum(chunkChecksumStr));
				}
				else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/multiChunks/multiChunk")) {
					String multChunkIdStr = attributes.getValue("id");
					MultiChunkId multiChunkId = MultiChunkId.parseMultiChunkId(multChunkIdStr);							
					
					if (multiChunkId == null) {
						throw new SAXException("Cannot read ID from multichunk " + multChunkIdStr);
					}
					
					multiChunk = new MultiChunkEntry(multiChunkId);					
				}			
				else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/multiChunks/multiChunk/chunkRefs/chunkRef")) {
					String chunkChecksumStr = attributes.getValue("ref");
	
					multiChunk.addChunk(ChunkChecksum.parseChunkChecksum(chunkChecksumStr));
				}
				else if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/fileHistories/fileHistory")) {
					String fileHistoryIdStr = attributes.getValue("id");
					FileHistoryId fileId = FileHistoryId.parseFileId(fileHistoryIdStr);
					fileHistory = new PartialFileHistory(fileId);
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
					String dosAttributes = attributes.getValue("dosattrs");
					String posixPermissions = attributes.getValue("posixperms");
					
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
						fileVersion.setChecksum(FileChecksum.parseFileChecksum(checksumStr));							
					}
					
					if (linkTarget != null) {
						fileVersion.setLinkTarget(linkTarget);
					}
					
					if (dosAttributes != null) {
						fileVersion.setDosAttributes(dosAttributes);
					}
					
					if (posixPermissions != null) {
						fileVersion.setPosixPermissions(posixPermissions);
					}
	
					fileHistory.addFileVersion(fileVersion);							
				}			
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
			else if (!headersOnly) {
				if (elementPath.equalsIgnoreCase("/database/databaseVersions/databaseVersion/fileContents/fileContent")) {
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
