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

import java.util.Map;

import org.syncany.gui.MainGUI;
import org.syncany.gui.messaging.event.InitCommandEvent;
import org.syncany.gui.messaging.event.SyncyngEvent;
import org.syncany.gui.messaging.event.SyncyngEvent.SyncyngState;
import org.syncany.gui.messaging.event.WatchUpdateEvent;
import org.syncany.util.JsonHelper;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class DaemonMessagesHandler {
	
	public void handleReceivedMessage(String message) {
		Map<String, Object> parameters = JsonHelper.fromStringToMap(message);
		String action = (String) parameters.get("action");
		String clientId = (String) parameters.get("client_id");
		String clientType = (String) parameters.get("client_type"); // syncany-gui

		switch (action) {
		case "daemon_command_result":
			// test if daemon update
			if (MainGUI.getClientIdentification().equals(clientId) && clientType.equals("syncany-gui")) {
				InitCommandEvent ce = new InitCommandEvent(
					(String) parameters.get("command_id"), 
					(String) parameters.get("result"),
					(String) parameters.get("share_link"), 
					(String) parameters.get("localFolder"), 
					"yes".equals((String) parameters.get("share_link_encrypted"))
				);
				EventManager.post(ce);
			}
			break;

		case "update_watched_folders":
			// TODO[medium]: try not to use unsafe casting .....
			WatchUpdateEvent wue = new WatchUpdateEvent((Map<String, Map<String, String>>) parameters.get("folders"));
			EventManager.post(wue);
			break;

		case "update_syncing_state":
			String syncingState = (String) parameters.get("syncing_state");
			SyncyngEvent se = new SyncyngEvent();
			if (syncingState.equals("syncing")) {
				se.setState(SyncyngState.SYNCING);
			}
			else if (syncingState.equals("in-sync")) {
				se.setState(SyncyngState.SYNCED);
			}
			EventManager.post(se);
			break;
		}
	}
}
