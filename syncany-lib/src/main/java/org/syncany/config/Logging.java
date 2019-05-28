/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import java.io.InputStream;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * The logging class offers convenience functions to initialize and update the
 * application's log options. 
 * 
 * <p>In particular, it can load the log properties either from a resource or a
 * local file on the file system (<code>logging.properties</code>. If a local file is
 * present, it is preferred over the JAR resource.
 * 
 * <p>To initialize logging, the {@link #init()} method can be called in the 
 * <code>static</code> block of a class, e.g.
 * 
 * <pre>
 *   static {
 *     Logging.init();
 *   }
 * </pre>
 *  
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
public class Logging {
	private static final String LOG_PROPERTIES_JAR_RESOURCE = "/" + Logging.class.getPackage().getName().replace(".", "/") + "/logging.properties";	
	private static AtomicBoolean loggingInitialized = new AtomicBoolean(false);
	
	public synchronized static void init() {
		if (loggingInitialized.get()) {
			return;
		}

		// Turn off unwanted loggers (evil libraries and such) 
		disableUnwantedLoggers();		
				
		// Load logging.properties
    	try {
    		// Use file if exists, else use file embedded in JAR
    		InputStream logConfigInputStream = Config.class.getResourceAsStream(LOG_PROPERTIES_JAR_RESOURCE);
    		
    	    if (logConfigInputStream != null) {
    	    	LogManager.getLogManager().readConfiguration(logConfigInputStream);
    	    }
    	    
    	    loggingInitialized.set(true);
    	}
    	catch (Exception e) {
    	    Logger.getAnonymousLogger().severe("Could not load logging.properties file from resource "+LOG_PROPERTIES_JAR_RESOURCE);
    	    Logger.getAnonymousLogger().severe(e.getMessage());
    	    
    	    e.printStackTrace();
    	}		
	}	
	
	private static void disableUnwantedLoggers() {
		System.setProperty("hsqldb.reconfig_logging", "false");
		
		if (Logger.getLogger("sun.awt.X11.timeoutTask.XToolkit") != null) {
			Logger.getLogger("sun.awt.X11.timeoutTask.XToolkit").setLevel(Level.OFF);
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
		
		// Make sure the unwanted loggers stay quiet
		disableUnwantedLoggers();
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
