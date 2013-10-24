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
package org.syncany.chunk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.util.StringUtil;

/**
 *
 * @author pheckel
 */
public abstract class Transformer {
	private static final Logger logger = Logger.getLogger(Transformer.class.getSimpleName());
    protected Transformer nextTransformer;
    
    public Transformer() {
        this(null);
    }
    
    public Transformer(Transformer nextTransformer) {
        this.nextTransformer = nextTransformer;
    }
    
    public Transformer getNextTransformer() {
		return nextTransformer;
	}

	public void setNextTransformer(Transformer nextTransformer) {
		this.nextTransformer = nextTransformer;
	}

	public abstract void init(Map<String, String> settings) throws Exception;
    public abstract OutputStream createOutputStream(OutputStream out) throws IOException;
    public abstract InputStream createInputStream(InputStream in) throws IOException;
    
    @Override
    public abstract String toString();
    
    public static Transformer getInstance(String operation) throws Exception {
		String thisPackage = Transformer.class.getPackage().getName();
		String camelCaseName = StringUtil.toCamelCase(operation);
		String fqClassName = thisPackage+"."+camelCaseName+"Transformer";
		
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
}
