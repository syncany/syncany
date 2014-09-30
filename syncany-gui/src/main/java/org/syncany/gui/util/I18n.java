package org.syncany.gui.util;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import net.sf.corn.cps.CPScanner;
import net.sf.corn.cps.ResourceFilter;


/**
 * Internationalisation Class
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class I18n implements Serializable {
    private static final long serialVersionUID = -4871656417610600492L;
	private static final Logger log = Logger.getLogger(I18n.class.getSimpleName());
    
    private static final HashMap<Locale, Properties> bundles = new HashMap<Locale, Properties>();
	private static final List<String> BUNDLE_NAMES = new ArrayList<String>();
	private static final List<String> BUNDLE_FILTERS= new ArrayList<String>();
	private static final String defaultBundleLauguage = "en";
	private static final String defaultBundleCountry = "US";
	
	public static void registerBundleName(String bundle){
		BUNDLE_NAMES.add(bundle);
	}
	
	public static void registerBundleFilter(String filter) {
		BUNDLE_FILTERS.add(filter);
	}
	
	private static HashMap<Locale, Properties> getBundles() {
		return bundles;
	}

	private static void loadBundle(Locale l) {
		//Build resources for bundle names
		for (String bundleName : BUNDLE_NAMES){
			ResourceBundle bun;

			try{
				bun = ResourceBundle.getBundle(bundleName, l, ClassLoader.getSystemClassLoader());
				buildResourceBundle(bun, l);
			}
			catch (MissingResourceException e) {
				Locale defaultLocale = new Locale(defaultBundleLauguage, defaultBundleCountry);
				bun = ResourceBundle.getBundle(bundleName, defaultLocale, ClassLoader.getSystemClassLoader());
				buildResourceBundle(bun, l);
			}
		}
		
		//Build resources for bundle filters
		for (String filter : BUNDLE_FILTERS){
			List<URL> resources = CPScanner.scanResources(new ResourceFilter().resourceName(filter + ".properties"));

			boolean containsRequiredLocaleFile = false;
			
			for (URL url : resources){
				String[] tokens = url.getFile().split("/");
				String fileName = tokens[tokens.length-1];
				
				if (fileName.contains(l.toString())){
					containsRequiredLocaleFile = true;
				}
			}
			
			if (containsRequiredLocaleFile){
				for (URL url : resources){
					buildResourceBundle(url, l);
				}
			}
			else{
				Locale defaultLocale = new Locale(defaultBundleLauguage, defaultBundleCountry);
				for (URL url : resources){
					buildResourceBundle(url, defaultLocale, l);
				}
			}
		}
	}
	
	private static void buildResourceBundle(URL url, Locale defaultLocale, Locale originalLocale) {
		Properties ap = new Properties();
		
		String[] tokens = url.getFile().split("/");
		String fileName = tokens[tokens.length-1];
		
		if (fileName.contains(defaultLocale.toString())){
			try {
				ap.load(url.openStream());
			}
			catch (IOException e) {
				log.warning("error loading file " + url.toString());
			}
		}
		
		if (getBundles().containsKey(originalLocale)){
			Properties oldProperties = getBundles().get(originalLocale);
			ap.putAll(oldProperties);
		}
		
		getBundles().put(originalLocale, ap);
	}

	private static void buildResourceBundle(URL url, Locale l){
		buildResourceBundle(url, l, l);
	}
	
	private static void buildResourceBundle(ResourceBundle bun, Locale l) {
		Properties ap = new Properties();
		for (String s : bun.keySet()) {
			ap.put(s, bun.getString(s));
		}

		if (getBundles().containsKey(l)){
			Properties oldProperties = getBundles().get(l);
			ap.putAll(oldProperties);
		}
		
		getBundles().put(l, ap);
	}

	public static String getString(String key){
		return getString(key, false);
	}
	
	public static String getString(String key, String...args){
		return getString(key, false, args);
	}
	
	/**
	 * Returns the translation for the key String, given the selected
	 * defaultLocale
	 * 
	 * @param key
	 * @return the localised value corresponding to the key
	 */
	public static String getString(String key, boolean semicolon) {
        if (!getBundles().containsKey(Locale.getDefault())){
            loadBundle(Locale.getDefault());
        }

        try {
        	Properties ap = bundles.get(Locale.getDefault());
			return ap == null ? key : ap.getProperty(key).trim() + (semicolon ? " :" : "");
		} 
        catch (NullPointerException e) {
			if (key != null && !key.isEmpty()) {
				log.warning(Locale.getDefault() + " : key " + key + " not translated");
			}
			return key;
		}
	}
	
	public static String getString(String key, boolean semicolon, String...args){
		String s = getString(key, semicolon);
		String tmp;
		
		for (int i = 0 ; i < args.length ; i ++){
			tmp = args[i];
			
			if (tmp != null && tmp.length() > 0)
				s = s.replace("{" + i + "}", args[i]);
		}
		
		return s;
	}
}