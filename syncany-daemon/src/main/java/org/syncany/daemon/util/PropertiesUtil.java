package org.syncany.daemon.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class PropertiesUtil {

    /**
     * Load a Properties File
     * @param propsFile
     * @return Properties
     * @throws IOException
     */
    public static Properties load(String fileName) throws IOException {
    	File propsFile = new File(fileName);
        Properties props = new Properties();
        FileInputStream fis = new FileInputStream(propsFile);
        props.load(fis);    
        fis.close();
        return props;
    }
    
    public static void put(String fileName, String key, String value) throws Exception{
    	File f = new File(fileName);
    	Properties p = load(fileName);
    	p.put(key, value);
    	p.store(new FileWriter(f), null);
    }
    
    public static String get(String fileName, String key) throws Exception{
    	Properties p = load(fileName);
    	return p.getProperty(key);
    }
}