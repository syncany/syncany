/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.reflections.Reflections;

public class Logging {
	private static boolean loggingInitialized = false;
	
	public synchronized static void init() {
		if (loggingInitialized) {
			return;
		}

		// Turn off INFO message of Reflections library (dirty, but the only way!) 
		Reflections.log = null;
		
		// Load logging.properties
    	try {
    		// Use file if exists, else use file embedded in JAR
    		File logConfig = new File("logging.properties");
    		InputStream logConfigInputStream;
    		
    		if (logConfig.exists() && logConfig.canRead()) {
    			logConfigInputStream = new FileInputStream(new File("logging.properties"));
    		}
    		else {
    			logConfigInputStream = Config.class.getResourceAsStream("/logging.properties");
    		}
    		
    	    if (logConfigInputStream != null) {
    	    	LogManager.getLogManager().readConfiguration(logConfigInputStream);
    	    }
    	    
    	    loggingInitialized = true;
    	}
    	catch (Exception e) {
    	    Logger.getAnonymousLogger().severe("Could not load logging.properties file from file system or JAR.");
    	    Logger.getAnonymousLogger().severe(e.getMessage());
    	    
    	    e.printStackTrace();
    	}
		
	}	
	
	public static void setGlobalLogLevel(Level targetLogLevel) {
		for (Enumeration<String> loggerNames = LogManager.getLogManager().getLoggerNames(); loggerNames.hasMoreElements(); ) {
            String loggerName = loggerNames.nextElement();
            Logger logger = LogManager.getLogManager().getLogger(loggerName);
            
            if (logger != null) {
            	logger.setLevel(targetLogLevel);
            }
		}	
		
		for (Handler handler : Logger.getLogger("").getHandlers()) {
			handler.setLevel(targetLogLevel);
		}
		
		Logger.getLogger("").setLevel(targetLogLevel);		
	}
	
	public static void addGlobalHandler(Handler targetHandler) {
		Logger.getLogger("").addHandler(targetHandler);
	}
		
	public static void disableLogging() {
		LogManager.getLogManager().reset();
		
		setGlobalLogLevel(Level.OFF);
		
		while (Logger.getLogger("").getHandlers().length > 0) {
			Logger.getLogger("").removeHandler(Logger.getLogger("").getHandlers()[0]);
		}
	}	
}
