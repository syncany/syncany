package org.syncany.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Logger;


/**
 * Internationalisation Class
 * @author Vincent Wiencek
 *
 */
public class I18n implements Serializable {
    private static final long serialVersionUID = -4871656417610600492L;
	private static final Logger log = Logger.getLogger(I18n.class.getSimpleName());
    
    private static final HashMap<Locale, Properties> bundles = new HashMap<Locale, Properties>();
	private static final List<String> BUNDLE_NAMES = new ArrayList<String>();
	private static final String defaultBundleLauguage = "en";
	private static final String defaultBundleCountry = "GB";
	
	public static void registerBundleName(String bundle){
		BUNDLE_NAMES.add(bundle);
	}
	
	private static HashMap<Locale, Properties> getBundles() {
		return bundles;
	}

	private static void loadBundle(Locale l) {
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

	/**
	 * Returns the translation for the key String, given the selected
	 * defaultLocale
	 * 
	 * @param key
	 * @return the localised value corresponding to the key
	 */
	public static String getString(String key) {
        if (!getBundles().containsKey(Locale.getDefault())){
            loadBundle(Locale.getDefault());
        }

        try {
        	Properties ap = bundles.get(Locale.getDefault());
			return ap == null ? key : ap.getProperty(key).trim();
		} 
        catch (NullPointerException e) {
			if (key != null && !key.isEmpty()) {
				log.warning(Locale.getDefault() + " : key " + key + " not translated");
			}
			return key;
		}
	}
	
	public static String getString(String key, String...args){
		String s = getString(key);
		String tmp;
		
		for (int i = 0 ; i < args.length ; i ++){
			tmp = args[i];
			
			if (tmp != null && tmp.length() > 0)
				s = s.replace("{" + i + "}", args[i]);
		}
		
		return s;
	}
}