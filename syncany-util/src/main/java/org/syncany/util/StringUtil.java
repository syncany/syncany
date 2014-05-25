/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.util;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.xml.bind.DatatypeConverter;

/**
 * Utility class for common application string functions.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class StringUtil {   
	private static final String MACHINE_NAME_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private static final int MACHINE_NAME_LENGTH = 20;
	
	private static Random random = new Random();

	/**
	 * Transforms a string to a camel case representation, including the
	 * first character.
	 * 
	 * <p>Examples:
	 * <ul>
	 *  <li><tt>toCamelCase("hello world") -&gt; "HelloWorld"</tt></li>
	 *  <li><tt>toCamelCase("hello_world") -&gt; "HelloWorld"</tt></li>
	 * </ul>
	 */
    public static String toCamelCase(String str) {
        StringBuilder sb = new StringBuilder();

        for (String s : str.split("[-_ ]")) {
        	if (s.length() > 0) {
	            sb.append(Character.toUpperCase(s.charAt(0)));
	
	            if (s.length() > 1) {
	                sb.append(s.substring(1, s.length()).toLowerCase());
	            }
        	}
        }

        return sb.toString();
    }
    
    /**
     * Converts a byte array to a lower case hex representation.
     * If the given byte array is <tt>null</tt>, an empty string is returned.
     */
    public static String toHex(byte[] bytes) {
    	if (bytes == null) {
    		return "";
    	}
    	else {
    		return DatatypeConverter.printHexBinary(bytes).toLowerCase();
    	}
    }
    
    /**
     * Creates byte array from a hex represented string.
     */
    public static byte[] fromHex(String s) {
    	return DatatypeConverter.parseHexBinary(s); // fast!    	
    }
    
    public static byte[] toBytesUTF8(String s) {
    	try {
			return s.getBytes("UTF-8");
		} 
    	catch (UnsupportedEncodingException e) {
			throw new RuntimeException("JVM does not support UTF-8 encoding.", e);
		}
    }
    
    /**
     * Generates a random machine name of length 20. Only uses characters 
     * A-Z/a-z (in order to always create valid serialized vector clock representations)  
     */
	public static String createRandomMachineName() {
		StringBuilder sb = new StringBuilder(MACHINE_NAME_LENGTH);
		
		for (int i = 0; i < MACHINE_NAME_LENGTH; i++) {
			sb.append(MACHINE_NAME_CHARS.charAt(random.nextInt(MACHINE_NAME_CHARS.length())));
		}
		
		return sb.toString();
	}
	
	public static <T> String join(List<T> objects, String delimiter, StringJoinListener<T> listener) {
		StringBuilder objectsStr = new StringBuilder();
		
		for (int i=0; i<objects.size(); i++) {
			if (listener != null) {
				objectsStr.append(listener.getString(objects.get(i)));
			}
			else {
				objectsStr.append(objects.get(i).toString());
			}
			
			if (i < objects.size()-1) { 
				objectsStr.append(delimiter);
			}			
		}
		
		return objectsStr.toString();
	}   
	
	public static <T> String join(List<T> objects, String delimiter) {
		return join(objects, delimiter, null);
	} 
	
	public static <T> String join(T[] objects, String delimiter, StringJoinListener<T> listener) {
		return join(Arrays.asList(objects), delimiter, listener);
	}   
	
	public static <T> String join(T[] objects, String delimiter) {
		return join(Arrays.asList(objects), delimiter, null);
	}   
	
	public static interface StringJoinListener<T> {
		public String getString(T object);
	}	
}
