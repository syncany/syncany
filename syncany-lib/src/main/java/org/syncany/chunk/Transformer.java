/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com>
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.util.StringUtil;

/**
 * A transformer combines one or many stream-transforming {@link OutputStream}s and {@link InputStream}s.
 * Implementations might provide functionality to encrypt or compress output streams, and to decrypt
 * or uncompress a corresponding  input stream.
 *
 * <p>Transformers can be chained in order to allow multiple consecutive output-/input stream
 * transformers to be applied to a stream.
 *
 * <p>A transformer can be instantiated using its implementation-specific constructor, or by calling
 * its default constructor and initializing it using the {@link #init(Map) init()} method. Depending
 * on the implementation, varying settings must be passed.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class Transformer {
	private static final Logger logger = Logger.getLogger(Transformer.class.getSimpleName());
	protected Transformer nextTransformer;

	/**
	 * Creates a new transformer object (no next transformer)
	 */
	public Transformer() {
		this(null);
	}

	/**
	 * Create a new transformer, with a nested/chained transformer that will be 
	 * be applied after this transformer.
	 * 
	 * @param nextTransformer The next transformer (to be applied after this transformer)
	 */
	public Transformer(Transformer nextTransformer) {
		this.nextTransformer = nextTransformer;
	}

	/**
	 * If a transformer is instantiated via the default constructor (e.g. via a config file),
	 * it must be initialized using this method. The settings passed to the method depend
	 * on the implementation of the transformer.
	 *
	 * @param settings Implementation-specific setting map
	 * @throws Exception If the given settings are invalid or insufficient for instantiation
	 */
	public abstract void init(Map<String, String> settings) throws Exception;

	/**
	 * Create a stream-transforming {@link OutputStream}. Depending on the implementation, the
	 * bytes written to the output stream might be encrypted, compressed, etc.
	 *
	 * @param out Original output stream which is transformed by this transformer
	 * @return Returns a transformed output stream
	 * @throws IOException If an exception occurs when instantiating or writing to the stream
	 */
	public abstract OutputStream createOutputStream(OutputStream out) throws IOException;

	/**
	 * Creates a strea-transforming {@link InputStream}. Depending on the implementation, the
	 * bytes read from the input stream are uncompressed, decrypted, etc.
	 *  
	 * @param in Original input stream which is transformed by this transformer 
	 * @return Returns a transformed input stream
	 * @throws IOException If an exception occurs when instantiating or reading from the stream
	 */
	public abstract InputStream createInputStream(InputStream in) throws IOException;

	/**
	 * An implementation of a transformer must override this method to identify the 
	 * type of transformer and/or its settings.
	 */
	@Override
	public abstract String toString();

	/**
	 * Instantiates a transformer by its name using the default constructor. After creating
	 * a new transformer, it must be initialized using the {@link #init(Map) init()} method.  
	 * 
	 * <p>The given type attribute is mapped to fully qualified class name (FQCN) of the form
	 * <tt>org.syncany.chunk.XTransformer</tt>, where <tt>X</tt> is the camel-cased type
	 * attribute.  
	 * 
	 * @param type Type/name of the transformer (corresponds to its camel case class name)
	 * @return Returns a new transformer
	 * @throws Exception If the FQCN cannot be found or the class cannot be instantiated
	 */
	public static Transformer getInstance(String type) throws Exception {
		String thisPackage = Transformer.class.getPackage().getName();
		String camelCaseName = StringUtil.toCamelCase(type);
		String fqClassName = thisPackage + "." + camelCaseName + Transformer.class.getSimpleName();

		// Try to load!
		try {
			Class<?> clazz = Class.forName(fqClassName);
			return (Transformer) clazz.newInstance();
		}
		catch (Exception ex) {
			logger.log(Level.INFO, "Could not find operation FQCN " + fqClassName, ex);
			return null;
		}
	}

	public void setNextTransformer(Transformer nextTransformer) {
		this.nextTransformer = nextTransformer;
	}
}
