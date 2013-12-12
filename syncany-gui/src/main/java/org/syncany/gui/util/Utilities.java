
package org.syncany.gui.util;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * 
 * Misc utils class
 * @author Vincent Wiencek
 *
 */
public class Utilities implements Serializable {
	private static final long serialVersionUID = -4430785172094845393L;
	private static final Logger log = Logger.getLogger(Utilities.class.getSimpleName());
	
	public static String objToString(Object o){
		String value;
		if (o instanceof String){
			value = (String)o;
		}
		else if (o instanceof Boolean){
			value = ((Boolean)o).toString();
		}
		else if (o instanceof Color){
			value = ((Color)o).getRGB()+"";
		}
		else if (o instanceof Number){
			value = ((Number)o).toString();
		} 
		else{
			value = o.toString();
		}
		return value;
	}

	public static InputStream getUrlAsStream(String fileName) throws IOException{
		URL u = getUrl(fileName);
		
		if (u != null){
			return u.openStream();
		}
		else{
			return Utilities.class.getClassLoader().getResourceAsStream(fileName);
		}
	}
	
	public static URL getUrl(String fileName){
		URL url = null;
		String[] prefix = {"", "/", "./"};
		
		for(String p : prefix){
			String ss = p + fileName;
			
			if (url == null){	
				url = Thread.currentThread().getContextClassLoader().getResource(ss);
			}
			if (url == null){
				url = ClassLoader.getSystemResource(ss);
			}
			if (url == null){
				url = ClassLoader.getSystemClassLoader().getResource(ss);
			}
		}
		
		if (url == null){
			try {
				File f = new File(fileName);
				
				if (f.exists())
					url = new File(fileName).toURI().toURL();
			} catch (MalformedURLException e) {
				log.warning("Error in trying to get url for file " + fileName);
			}
		}
		
		if (url != null){
			log.fine("File '" + fileName + "' found at URL "+url.toString());
		}
		else{
			log.fine("File '" + fileName + "' not found");
		}
		
		return url;	
	}
	
	public static String formatStackTrace(Throwable t){
		StackTraceElement elements[] = t.getStackTrace();
		StringBuilder sb = new StringBuilder(t.toString()).append("\n");
		for (int i=0, n=elements.length; i<n; i++) {
			sb.append("\tat ");
			
			sb.append(elements[i].getClassName()).append(".java");
			sb.append(" : "); 
		    sb.append(elements[i].getLineNumber()).append(" ==> ");
		    sb.append(elements[i].getMethodName()).append("()");
		    
		    sb.append("\n");
		}
		
		return sb.toString();
	}
	
	/**
	 * 
	 * @param f
	 * @return
	 * @throws IOException
	 */
	public static Properties load(File f) throws IOException {
    	Properties props = new Properties();
        FileInputStream fis = new FileInputStream(f);
        props.load(fis);    
        fis.close();
        return props;
	}
	
    /**
     * Load a Properties File
     * @param fileName the name of the file to load
     * @return Properties
     * @throws IOException
     */
    public static Properties load(String fileName) throws IOException{
    	URL u = getUrl(fileName);
    	Properties props = new Properties();
        props.load(u.openStream());    
        return props;
    }
    
    /**
     * Returns a String containing System Informations
     * @return
     */
    public static String getSystemInformations(){
    	Properties p = System.getProperties();
    	StringBuilder sb = new StringBuilder();
    	
    	for (Object key : p.keySet()){
    		String s = (String) key;
    		sb.append(s).append(" = ").append(p.get(key)).append("\n");
    	}
		
		return sb.toString();
    }
}