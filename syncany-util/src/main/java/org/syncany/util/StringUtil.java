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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

/**
 * Utility class for common application string functions.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class StringUtil {   
	private static Pattern COMMAND_ARGS_PATTERN = Pattern.compile("([^'\"\\s]+)|\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"|\'([^'\\\\]*(?:\\\\.[^'\\\\]*)*)\'");
	
	/**
	 * Transforms a string to a camel case representation, including the
	 * first character.
	 * 
	 * <p>Examples:
	 * <ul>
	 *  <li><tt>toCamelCase("hello world") -&gt; "HelloWorld"</tt></li>
	 *  <li><tt>toCamelCase("hello_world") -&gt; "HelloWorld"</tt></li>
	 *  <li><tt>toCamelCase("hello_World") -&gt; "HelloWorld"</tt></li>
	 *  <li><tt>toCamelCase("helloWorld") -&gt; "HelloWorld"</tt></li>
	 *  <li><tt>toCamelCase("HelloWorld") -&gt; "HelloWorld"</tt></li>
	 * </ul>
	 */
    public static String toCamelCase(String str) {
        StringBuilder sb = new StringBuilder();

        for (String s : str.split("[-_ ]")) {
        	if (s.length() > 0) {
	            sb.append(Character.toUpperCase(s.charAt(0)));
	
	            if (s.length() > 1) {
	                sb.append(s.substring(1, s.length()));
	            }
        	}
        }

        return sb.toString();
    }
    
    /**
     * Transforms a string to underscore-delimited representation.
     * 
	 * <p>Examples:
	 * <ul>
	 *  <li><tt>toUnderScoreDelimited("HelloWorld") -&gt; "hello_world"</tt></li>
	 *  <li><tt>toUnderScoreDelimited("helloWorld") -&gt; "hello_world"</tt></li>
	 * </ul>
     */
    public static String toSnakeCase(String str) {
		StringBuilder sb = new StringBuilder();

        for (char c : str.toCharArray()) {   
        	if (Character.isLetter(c) || Character.isDigit(c)) {
        		if (Character.isUpperCase(c)) {
            		if (sb.length() > 0) {
            			sb.append("_");
            		}
            		
            		sb.append(Character.toLowerCase(c));
            	}
            	else {
            		sb.append(c);
            	}
        	}
        	else {
        		sb.append("_");
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
    
    /**
     * Creates a byte array from a given string, using the UTF-8
     * encoding. This calls {@link String#getBytes(java.nio.charset.Charset)} 
     * internally with "UTF-8" as charset.
     */
    public static byte[] toBytesUTF8(String s) {
    	try {
			return s.getBytes("UTF-8");
		} 
    	catch (UnsupportedEncodingException e) {
			throw new RuntimeException("JVM does not support UTF-8 encoding.", e);
		}
    }
    
    /**
     * Returns the count of the substring 
     */
    public static int substrCount(String haystack, String needle) {
    	int lastIndex = 0;
    	int count = 0;

    	if (needle != null && haystack != null) {
			while (lastIndex != -1) {
				lastIndex = haystack.indexOf(needle, lastIndex);
	
				if (lastIndex != -1) {
					count++;
					lastIndex += needle.length();
				}
	    	}
    	}
    	
		return count;
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

	public static List<String> splitCommandLineArgs(String commandLine) {		
		List<String> commandArgsList = new ArrayList<String>();
	    Matcher commandArgsMatcher = COMMAND_ARGS_PATTERN.matcher(commandLine);
	    
	    while (commandArgsMatcher.find()) {
	    	if (commandArgsMatcher.group(1) != null) {
	    		commandArgsList.add(commandArgsMatcher.group(1));
	    	}
	    	else if (commandArgsMatcher.group(2) != null) {
	    		commandArgsList.add(commandArgsMatcher.group(2).replace("\\", ""));
	    	}
	    	else if (commandArgsMatcher.group(3) != null) {
	    		commandArgsList.add(commandArgsMatcher.group(3).replace("\\", ""));
	    	}	    	
	    }	 		
	    
	    return commandArgsList;
	}
}
