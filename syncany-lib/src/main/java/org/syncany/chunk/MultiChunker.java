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
package org.syncany.chunk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.util.StringUtil;

/**
 * A multichunker combines a set of {@link Chunk}s into a single file. It can be implemented
 * by a simple container or archive format. The multichunker is used by the {@link Deduper}
 * to write multichunks, and by other parts of the application to read multichunks and
 * re-assemble files.
 * 
 * <p>The class supports two modes: 
 * 
 * <ul>
 * <li>When writing a {@link MultiChunker}, the {@link #createMultiChunk(byte[], OutputStream)}
 *     must be used. The method emits a new implementation-specific {@link MultiChunk} 
 *     to which new chunks can be added/written to.
 *      
 * <li>When reading a multichunk from a file or input stream, the {@link #createMultiChunk(InputStream)}
 *     or {@link #createMultiChunk(File)} must be used. The emitted multichunk object can be read from.
 * </ul>
 * 
 * <p><b>Important:</b> Implementations must make sure that when providing a readable multichunk,
 * the individual chunk objects must be randomly accessible. A sequential read (like with TAR, for
 * instance), is not sufficient for the quick processing required in the application.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
// TODO [low] The multichunk API is really odd; Think of something more sensible
public abstract class MultiChunker {

	/**
	 * Minimal multi chunk size in KB.
	 */
	public static final String PROPERTY_SIZE = "size";

	private static Logger logger = Logger.getLogger(MultiChunker.class.getSimpleName());

	protected int minMultiChunkSize; // in KB

	/**
	 * Creates new multichunker without setting the minimum size of a multichunk.
	 */
	public MultiChunker() {
		// Nothing.
	}

	/**
	 * Initializes the multichunker using a settings map. 
	 * <br>
	 * Required settings are: 
	 * <ul>
	 *  <li> key: {@link #PROPERTY_SIZE}, value: integer encoded as String 
	 * </ul>
	 */
	public void init(Map<String, String> settings) {
		String size = settings.get(PROPERTY_SIZE);
		
		if (size == null) {
			logger.log(Level.SEVERE, String.format("Property %s must not be null.", PROPERTY_SIZE));
			throw new IllegalArgumentException(String.format("Property %s must not be null.", PROPERTY_SIZE));
		}
		
		try {
			this.minMultiChunkSize = Integer.parseInt(size);
		}
		catch (NumberFormatException nfe) {
			logger.log(Level.SEVERE, String.format("Property %s could not be parsed as Integer.", PROPERTY_SIZE));
			throw new IllegalArgumentException(String.format("Property %s could not be parsed as Integer.", PROPERTY_SIZE));
		}
	}

	/**
	 * Creates a new multichunker, and sets the minimum size of a multichunk.
	 * 
	 * <p>Implementations should react on the minimum multichunk size by allowing
	 * at least the given amount of KBs to be written to a multichunk, and declaring
	 * a multichunk 'full' if this limit is reached.
	 * 
	 * @param minMultiChunkSize Minimum multichunk file size in kilo-bytes
	 */
	public MultiChunker(int minMultiChunkSize) {
		this.minMultiChunkSize = minMultiChunkSize;
	}

	/**
	 * Create a new multichunk in <b>write mode</b>.
	 * 
	 * <p>Using this method only allows writing to the returned multichunk. The resulting
	 * data will be written to the underlying output stream given in the parameter. 
	 *   
	 * @param id Identifier of the newly created multichunk 
	 * @param os Underlying output stream to write the new multichunk to
	 * @return Returns a new multichunk object which can only be used for writing 
	 * @throws IOException
	 */
	public abstract MultiChunk createMultiChunk(MultiChunkId id, OutputStream os) throws IOException;

	/**
	 * Open existing multichunk in <b>read mode</b> using an underlying input stream.
	 * 
	 * <p>Using this method only allows reading from the returned multichunk. The underlying
	 * input stream is opened and can be used to retrieve chunk data.
	 * 
	 * @param is InputStream to initialize an existing multichunk for read-operations only
	 * @return Returns an existing multichunk object that allows read operations only
	 */
	public abstract MultiChunk createMultiChunk(InputStream is);

	/**
	 * Open existing multichunk in <b>read mode</b> using an underlying file.
	 * 
	 * <p>Using this method only allows reading from the returned multichunk. The underlying
	 * input stream is opened and can be used to retrieve chunk data.
	 * 
	 * @param is InputStream to initialize an existing multichunk for read-operations only
	 * @return Returns an existing multichunk object that allows read operations only
	 */
	public abstract MultiChunk createMultiChunk(File file) throws IOException;

	/**
	 * Returns a comprehensive string representation of a multichunker
	 */
	public abstract String toString();

	/**
	 * Instantiates a multichunker by its name using the default constructor. 
	 * <br>
	 * After creating a new multichunker, it must be initialized using the 
	 * {@link #init(Map) init()} method. The given type attribute is mapped to fully 
	 * qualified class name (FQCN) of the form <tt>org.syncany.chunk.XMultiChunker</tt>, 
	 * where <tt>X</tt> is the camel-cased type attribute.  
	 * 
	 * @param type Type/name of the multichunker (corresponds to its camel case class name)
	 * @return a new multichunker
	 * @throws Exception If the FQCN cannot be found or the class cannot be instantiated
	 */
	public static MultiChunker getInstance(String type) {
		String thisPackage = MultiChunker.class.getPackage().getName();
		String camelCaseName = StringUtil.toCamelCase(type);
		String fqClassName = thisPackage + "." + camelCaseName + MultiChunker.class.getSimpleName();

		// Try to load!
		try {
			Class<?> clazz = Class.forName(fqClassName);
			return (MultiChunker) clazz.newInstance();
		}
		catch (Exception ex) {
			logger.log(Level.INFO, "Could not find multichunker FQCN " + fqClassName, ex);
			return null;
		}
	}
}
