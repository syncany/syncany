package org.syncany.gui.rest;

import java.util.logging.Logger;

/**
 * Class used to handle REST communication
 * with client server
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class ClientHandler {
	private static final Logger log = Logger.getLogger(ClientHandler.class.getSimpleName());
	
	public synchronized String handleMessage(String message){
		
		switch (message){
			case "a":
				//MainGUI.instance.getWatchPanel().update();
				break;
				
			case "b":
				//MainGUI.instance.getWatchPanel().update();
				break;
		}
		return null;
	}
}
