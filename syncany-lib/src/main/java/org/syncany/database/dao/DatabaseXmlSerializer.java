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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;

import org.syncany.chunk.Transformer;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.VectorClock;

/**
 * Serializes a {@link MemoryDatabase} or a list of {@link DatabaseVersion}s to an 
 * XML-based file format, using a {@link Transformer} to compress/encrypt the file 
 * before writing, and to decompress/decrypt it before reading.
 * 
 * <p>The class offers a variety of <code>save()</code> to serialize and store a memory
 * database to a file, and several <code>load()</code> methods to load them from disk.
 * 
 * <p>It uses a {@link DatabaseXmlWriter} to write XML files to disk and 
 * {@link DatabaseXmlParseHandler} to parse them while reading. 
 * 
 * @see DatabaseXmlParseHandler
 * @see DatabaseXmlWriter
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
public class DatabaseXmlSerializer {
	private static final Logger logger = Logger.getLogger(DatabaseXmlSerializer.class.getSimpleName());

	public enum DatabaseReadType {
		FULL, HEADER_ONLY
	}

	private Transformer transformer;

	public DatabaseXmlSerializer() {
		this(null);
	}

	public DatabaseXmlSerializer(Transformer transformer) {
		this.transformer = transformer;
	}

	public void save(List<DatabaseVersion> databaseVersions, File destinationFile) throws IOException {
		save(databaseVersions.iterator(), destinationFile);
	}

	public void save(Iterator<DatabaseVersion> databaseVersions, File destinationFile) throws IOException {
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
			new DatabaseXmlWriter(databaseVersions, out).write();
		}
		catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}

	public void load(MemoryDatabase db, File databaseFile, VectorClock fromVersion, VectorClock toVersion, DatabaseReadType readType)
			throws IOException {
		
		InputStream is;

		if (transformer == null) {
			is = new FileInputStream(databaseFile);
		}
		else {
			is = transformer.createInputStream(new FileInputStream(databaseFile));
		}

		try {
			logger.log(Level.INFO, "- Loading database ({0}) from file {1} ...", new Object[] { readType, databaseFile });

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();

			saxParser.parse(is, new DatabaseXmlParseHandler(db, fromVersion, toVersion, readType));
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

}
