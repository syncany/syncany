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
package org.syncany.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Collection;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class StringUtil {
    private static int minimum(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    /**
     * @see http://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java
     * @param str1
     * @param str2
     * @return
     */
    public static int computeLevenshteinDistance(CharSequence str1,
		    CharSequence str2) {
        int[][] distance = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++)
		    distance[i][0] = i;
        for (int j = 0; j <= str2.length(); j++)
		    distance[0][j] = j;

        for (int i = 1; i <= str1.length(); i++)
		    for (int j = 1; j <= str2.length(); j++)
			    distance[i][j] = minimum(
					    distance[i - 1][j] + 1,
					    distance[i][j - 1] + 1,
					    distance[i - 1][j - 1]
							    + ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0
									    : 1));

        return distance[str1.length()][str2.length()];
    }  
    
    public static String join(Collection<String> strings, String delimiter) {
        return join((String[]) strings.toArray(new String[0]), delimiter);
    }

    public static String join(Object[] objects, String delimiter) {
    StringBuilder result = new StringBuilder();

    for (int i=0; i<objects.length; i++) {
        result.append(objects[i]);

        if (i != objects.length-1)
        result.append(delimiter);
    }

        return result.toString();
    }

    public static String toCamelCase(String str){
        StringBuilder sb = new StringBuilder();

        for (String s : str.split("_")) {
            sb.append(Character.toUpperCase(s.charAt(0)));

            if (s.length() > 1) {
                sb.append(s.substring(1, s.length()).toLowerCase());
            }
        }

        return sb.toString();
    }
    
    public static String toHex(byte[] bytes) {
    	if (bytes == null) {
    		return "";
    	}
    	else {
	        BigInteger bi = new BigInteger(1, bytes);
	        return String.format("%0" + (bytes.length << 1) + "x", bi);
    	}
    }
    
    public static byte[] fromHex(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                + Character.digit(s.charAt(i+1), 16));
        }
        
        return data;        
    } 
            
    public static int count(String text, String search) {
        int count = 0;
        
        for (int fromIndex = 0; fromIndex > -1; count++) {
            fromIndex = text.indexOf(search, fromIndex + ((count > 0) ? 1 : 0));
        }
        
        return count - 1;                
    }    
    
    public static String readStream(InputStream in, String charsetName) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        // Performance tests say 4K is the fastest (sschellh)
        byte[] buf = new byte[4096];

        int len;
        while ((len = in.read(buf)) > 0) {
            bos.write(buf, 0, len);
        }

        in.close();
        return bos.toString(charsetName);
    }    
    
    public static Integer parseInt(String value, Integer defaultValue) {
        try {
            return Integer.parseInt(value);
        }        
        catch (Exception e) {
            return defaultValue;
        }
    }


}
