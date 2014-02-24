/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.gui.messaging;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.daemon.websocket.messages.DaemonMessage;
import org.syncany.daemon.websocket.messages.DaemonResultInitMessage;
import org.syncany.daemon.websocket.messages.DaemonWatchEvent;
import org.syncany.daemon.websocket.messages.DeamonResultConnectMessage;
import org.syncany.daemon.websocket.messages.DeamonWatchResultMessage;
import org.syncany.gui.MainGUI;
import org.syncany.gui.messaging.event.EventManager;
import org.syncany.util.JsonHelper;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class DaemonMessagesHandler {
	private static final Logger logger = Logger.getLogger(DaemonMessagesHandler.class.getSimpleName());
	
	public void handleReceivedMessage(String messageString) {
		DaemonMessage message = JsonHelper.fromStringToObject(messageString, DaemonMessage.class);
		
		switch (message.getAction()) {
			case "daemon_watch_event":
				DaemonWatchEvent dwe = JsonHelper.fromStringToObject(messageString, DaemonWatchEvent.class);
				logger.log(Level.FINE, "event :" + dwe.getEvent());
				break;
			
			case "daemon_init_result":
				DaemonResultInitMessage r1 = JsonHelper.fromStringToObject(messageString, DaemonResultInitMessage.class);
				// test if daemon update
				if (MainGUI.getClientIdentification().equals(r1.getClientId()) && r1.getClientType().equals("syncany-gui")) {
					EventManager.post(r1);
				}
				break;
				
			case "daemon_connect_result":
				DeamonResultConnectMessage r2 = JsonHelper.fromStringToObject(messageString, DeamonResultConnectMessage.class);
				// test if daemon update
				if (MainGUI.getClientIdentification().equals(r2.getClientId()) && r2.getClientType().equals("syncany-gui")) {
					EventManager.post(r2);
				}
				break;
	
			case "update_watched_folders":
				DeamonWatchResultMessage rr = JsonHelper.fromStringToObject(messageString, DeamonWatchResultMessage.class);
				EventManager.post(rr);
				break;

		}
	}
}
