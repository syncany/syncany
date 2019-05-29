/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.database.dao;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.codec.binary.Base64;
import org.syncany.chunk.Chunk;
import org.syncany.chunk.MultiChunk;
import org.syncany.database.ChunkEntry;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.VectorClock;
import org.syncany.util.StringUtil;

/**
 * This class uses an {@link XMLStreamWriter} to output the given {@link DatabaseVersion}s
 * to a {@link PrintWriter} (or file). Database versions are written sequentially, i.e. 
 * according to their position in the given iterator. 
 * 
 * <p>A written file includes a representation of the entire database version, including
 * {@link DatabaseVersionHeader}, {@link PartialFileHistory}, {@link FileVersion}, 
 * {@link FileContent}, {@link Chunk} and {@link MultiChunk}.
 * 
 * @see DatabaseXmlSerializer
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
public class DatabaseXmlWriter {
	private static final Logger logger = Logger.getLogger(DatabaseXmlWriter.class.getSimpleName());
	
	private static final Pattern XML_RESTRICTED_CHARS_PATTERN = Pattern.compile("[\u0001-\u0008]|[\u000B-\u000C]|[\u000E-\u001F]|[\u007F-\u0084]|[\u0086-\u009F]");
	private static final int XML_FORMAT_VERSION = 1;
	
	private Iterator<DatabaseVersion> databaseVersions;
	private PrintWriter out;
	
	public DatabaseXmlWriter(Iterator<DatabaseVersion> databaseVersions, PrintWriter out) {
		this.databaseVersions = databaseVersions;
		this.out = out;
	}
	
	public void write() throws XMLStreamException, IOException {
		IndentXmlStreamWriter xmlOut = new IndentXmlStreamWriter(out);
		
		xmlOut.writeStartDocument();
		
		xmlOut.writeStartElement("database");
		xmlOut.writeAttribute("version", XML_FORMAT_VERSION);
		 
		xmlOut.writeStartElement("databaseVersions");
		 			
		while (databaseVersions.hasNext()) {
			DatabaseVersion databaseVersion = databaseVersions.next();
			
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
				xmlOut.writeAttribute("size", multiChunk.getSize());
			
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
			xmlOut.writeAttribute("id", fileHistory.getFileHistoryId().toString());
			
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
				
				if (containsXmlRestrictedChars(fileVersion.getPath())) {
					xmlOut.writeAttribute("pathEncoded", encodeXmlRestrictedChars(fileVersion.getPath()));
				}
				else {
					xmlOut.writeAttribute("path", fileVersion.getPath());	
				}
				
				xmlOut.writeAttribute("size", fileVersion.getSize());
				xmlOut.writeAttribute("lastModified", fileVersion.getLastModified().getTime());						
				
				if (fileVersion.getLinkTarget() != null) {
					xmlOut.writeAttribute("linkTarget", fileVersion.getLinkTarget());
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
	
	private String encodeXmlRestrictedChars(String str) {
		return Base64.encodeBase64String(StringUtil.toBytesUTF8(str));
	}

	/**
	 * Detects disallowed characters as per the XML 1.1 definition
	 * at http://www.w3.org/TR/xml11/#charsets
	 */
	private boolean containsXmlRestrictedChars(String str) {
		return XML_RESTRICTED_CHARS_PATTERN.matcher(str).find();
	}

	/**
	 * Wraps an {@link XMLStreamWriter} class to write XML data to
	 * the given {@link Writer}. 
	 * 
	 * <p>The class extends the regular xml stream writer to add 
	 * tab-based indents to the structure. The output is well-formatted
	 * human-readable XML. 
	 */
	public static class IndentXmlStreamWriter {
		private int indent;
		private XMLStreamWriter out;
		
		public IndentXmlStreamWriter(Writer out) throws XMLStreamException {
			XMLOutputFactory factory = XMLOutputFactory.newInstance();
			
			this.indent = 0;
			this.out = factory.createXMLStreamWriter(out);
		}
		
		private void writeStartDocument() throws XMLStreamException {
			out.writeStartDocument();
		}

		private void writeStartElement(String s) throws XMLStreamException {
			writeNewLineAndIndent(indent++);
			out.writeStartElement(s);	
		}
		
		private void writeEmptyElement(String s) throws XMLStreamException {
			writeNewLineAndIndent(indent);			
			out.writeEmptyElement(s);	
		}
		
		private void writeAttribute(String name, String value) throws XMLStreamException {
			out.writeAttribute(name, value);
		}
		
		private void writeAttribute(String name, int value) throws XMLStreamException {
			out.writeAttribute(name, Integer.toString(value));
		}
		
		private void writeAttribute(String name, long value) throws XMLStreamException {
			out.writeAttribute(name, Long.toString(value));
		}
		
		private void writeEndElement() throws XMLStreamException {			
			writeNewLineAndIndent(--indent);
			out.writeEndElement();			
		}
		
		private void writeEndDocument() throws XMLStreamException {
			out.writeEndDocument();
		}
		
		private void close() throws XMLStreamException {
			out.close();
		}
		
		private void flush() throws XMLStreamException {
			out.flush();
		}	
		
		private void writeNewLineAndIndent(int indent) throws XMLStreamException {
			out.writeCharacters("\n");			
			
			for (int i=0; i<indent; i++) {
				out.writeCharacters("\t");
			}
		}
	}	
}
