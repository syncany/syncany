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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.JarURLConnection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This utility class was based originally on <a href="private.php?do=newpm&u=47838">Daniel Le Berre</a>'s
 * <code>RTSI</code> class. This class can be called in different modes, but the principal use
 * is to determine what subclasses/implementations of a given class/interface exist in the current
 * runtime environment.
 *
 * <p>Extremely slimmed down (2011, PC Heckel)
 *
 * @author Daniel Le Berre, Elliott Wade, Philipp C. Heckel
 * @see http://www.velocityreviews.com/forums/t137693-find-all-implementing-classes-in-classpath.html
 */
public class ClasspathUtil {
    public enum ListType { CLASS, PACKAGE, BOTH };

    private final static Comparator<URL> URL_COMPARATOR = new Comparator<URL>() {
        @Override public int compare(URL u1, URL u2) { return String.valueOf(u1).compareTo(String.valueOf(u2)); }};

    public static Map<URL, String> getClasspathClasses() {
        return getClasspathLocations(ListType.CLASS);
    }

    public static Map<URL, String> getClasspathPackages() {
        return getClasspathLocations(ListType.PACKAGE);
    }

    public static Map<URL, String> getClasspathEntries() {
        return getClasspathLocations(ListType.BOTH);
    }

    /**
     * Determine every URL location defined by the current classpath, and
     * it's associated package name.
     */
    private static Map<URL, String> getClasspathLocations(ListType type) {
        Map<URL, String> map = new TreeMap<URL, String>(URL_COMPARATOR);

        String classpath = System.getProperty("java.class.path");
        //System.out.println ("classpath=" + classpath);
        
        StringTokenizer st = new StringTokenizer(classpath, File.pathSeparator);
        while (st.hasMoreTokens()) {
            String path = st.nextToken();
            //System.out.println("PATH =" +path);
            include(null, new File(path), type, map);
        }

        return map;
    } 

    private static void include(String name, File file, ListType type, Map<URL, String> map) {
        if (!file.exists()) 
            return;        
        
        if (!file.isDirectory()) {
            // could be a JAR file
            includeJar(file, type, map);
            return;
        }

        name = (name == null) ? "" : name + ".";

        // add subpackages
        File[] entries = file.listFiles();

        for (File entry : entries) {
            if (entry.isDirectory() && entry.getName().toUpperCase().equals("META-INF"))
                continue;

            if (!entry.isDirectory() && !entry.getName().endsWith(".class"))
                continue;
                        
            try {               
                // Add if in filter
                if ((entry.isDirectory() && type != ListType.CLASS) ||
                   (!entry.isDirectory() && type != ListType.PACKAGE)) {

                    String entryName = (entry.isDirectory()) ? name + entry.getName() : getFQCN(name + entry.getName());
                    URL entryURL = new URL("file://" + entry.getCanonicalPath());
                    map.put(entryURL, entryName);
                }
                
                // Include sub directories
                if (entry.isDirectory())
                    include(name + entry.getName(), entry, type, map);
            }
            catch (IOException ioe) {
                continue;
            }

        }
    }

    private static void includeJar(File file, ListType type, Map<URL, String> map) {
        if (file.isDirectory()) 
            return;        

        URL jarURL = null;
        JarFile jar = null;

        try {
            jarURL = new URL("file://" + file.getCanonicalPath());
            jarURL = new URL("jar:" + jarURL.toExternalForm() + "!/");

            JarURLConnection conn = (JarURLConnection) jarURL.openConnection();
            jar = conn.getJarFile();
        }
        catch (Exception e) {
            //System.err.println(e);
            // not a JAR or disk I/O error
            return;
        }

        if (jar == null || jarURL == null) 
            return;        

        // include the jar's "default" package (i.e. jar's root)
        map.put(jarURL, "");

        Enumeration<JarEntry> e = jar.entries();
        while (e.hasMoreElements()) {
            JarEntry entry = e.nextElement();

            if (entry.isDirectory()) {
                if (type == ListType.CLASS) continue;
                if (entry.getName().toUpperCase().equals("META-INF")) continue;
            }
            else {
                if (type == ListType.PACKAGE) continue;
                if (!entry.getName().endsWith(".class")) continue;
            }

            try {
                String entryName = (entry.isDirectory()) ? getPackageName(entry.getName()) : getFQCN(entry.getName());
                URL entryURL = new URL(jarURL.toExternalForm() + entry.getName());

                map.put(entryURL, entryName);
            }
            catch (IOException ioe) {
                continue;
            }         
        }
    }

    private static String getPackageName(String s) {
        if (s.startsWith("/")) 
            s = s.substring(1, s.length());

        if (s.endsWith("/")) 
            s = s.substring(0, s.length() - 1);
       
        return s.replace('/', '.');
    }

    private static String getFQCN(String s) {
        s = s.replace('/', '.');
        return s.replaceAll("\\.class$", "");

    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        for (Map.Entry<URL, String> m : ClasspathUtil.getClasspathPackages().entrySet()) {
            String className = m.getValue();

            if (!className.startsWith("org.syncany.connection.plugins"))
                continue;

          //  if (!className.endsWith("PluginInfo"))
            //    continue;
            
            System.out.println(m.getValue());

        }
        long duration = System.currentTimeMillis()-start;
        System.out.println("duration: "+duration+"ms");
    }

}
