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
package org.syncany;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.syncany.util.logging.LogFormatter;

import org.syncany.communication.CommunicationController;
import org.syncany.communication.CommunicationSocket;
import org.syncany.util.StringUtil;


/**
 * Main class for the Syncany client.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Syncany {
	
	private static final String configFile = "config.json";

    /** Main-Method for Syncany
     * @param args arguments for the Syncany daemon
     * 		args[0] should be port on which the GUI listens, if started with GUI
     * 		args[0] should be the path to the config file, when starting without GUI
     * 		if ( args[0] == null ) look for standard config file
     */
    public static void main(String[] args) {
    	// setup logging
    	Logger log = Logger.getLogger("");
    	for (Handler h : log.getHandlers()) {
    		h.setLevel(Level.ALL);
    		h.setFormatter(new LogFormatter());
    	}
        
        // process options/arguments
    	String argument = null;
    	if(args!=null && args.length>0) argument = args[0];
    	
    	Integer port = null;
    	try {
    		port = Integer.valueOf(argument);
    		Syncany.start(port);
    	} catch (Exception e) {
    		// argument was not a port
    		if(argument != null) {
        		// is pathToConfig
    			Syncany.start(argument);
    		} else {
    			// load default config
    			Syncany.start(System.getProperty("user.dir")+File.separator+configFile);
    		}
		}
    }
    
    
    /** Starting from GUI */
    public static void start(int port) {
    	/* Daemon connects to GUI and requests initialData */
		CommunicationSocket comSock = CommunicationSocket.getInstance();
		comSock.setPort(port);
		comSock.connect();
		CommunicationController.getInstance().requestInitialization();
    }
    
    /** Start with config and without GUI */
    public static void start(String pathToConfig) {
    	try {
    		StringBuffer fileData = new StringBuffer();
    		BufferedReader reader = new BufferedReader(new FileReader(pathToConfig));
    		char[] buf = new char[1024];
    		int numRead=0;
    		while((numRead=reader.read(buf)) != -1){
    			String readData = String.valueOf(buf, 0, numRead);
    			fileData.append(readData);
    		}
    		reader.close();
    		String json = fileData.toString();

    		// initialize & start Syncany        		
    		CommunicationController.getInstance().processInitialData(json);
    	}
        catch (Exception e) {
            System.err.println("ERROR: "+ e.getMessage());
            System.err.println(StringUtil.getStackTrace(e));
            System.exit(1);
        }
    }
    
    // TODO: only used for tests -> move to tests ?? 
    public static CommunicationController.Data getConfigData(String pathToConfig){
    	try {
    		StringBuffer fileData = new StringBuffer();
    		BufferedReader reader = new BufferedReader(new FileReader(pathToConfig));
    		char[] buf = new char[1024];
    		int numRead=0;
    		while((numRead=reader.read(buf)) != -1){
    			String readData = String.valueOf(buf, 0, numRead);
    			fileData.append(readData);
    		}
    		reader.close();
    		String json = fileData.toString();

    		// initialize Syncany        		
    		return CommunicationController.getInstance().getInitialData(json);
    	}
        catch (Exception e) {
            System.err.println("ERROR: "+ e.getMessage());
            System.err.println(StringUtil.getStackTrace(e));
            System.exit(1);
            
            return null;
        }
    }

}
