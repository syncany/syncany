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

import org.syncany.gui.Launcher;
import org.syncany.gui.messaging.InterfaceUpdate.InterfaceUpdateAction;
import org.syncany.util.JsonHelper;

/**
 * @author vincent
 *
 */
public class DaemonMessagesHandler {
	@SuppressWarnings("unchecked")
	public void handleReceivedMessage(String message) {
		Map<String, Object> parameters = JsonHelper.fromStringToMap(message);
		String action = (String)parameters.get("action");
		InterfaceUpdate iu;
		
		
		switch (action){
			case "daemon_update":
				//TEST SI C BON TOUT CA
				iu = new InterfaceUpdate(InterfaceUpdateAction.WIZARD_COMMAND_DONE, parameters);
			    Launcher.getEventBus().post(iu);
				break;
			
			case "update_watched_folders":
			    iu = new InterfaceUpdate(InterfaceUpdateAction.UPDATE_WATCHED_FOLDERS, (Map<String, Object>)parameters.get("folders"));
			    Launcher.getEventBus().post(iu);
				break;
		
			case "update_syncing_state":
				String syncingState = (String)parameters.get("syncing_state");
				if (syncingState.equals("syncing")){
					Launcher.getEventBus().post(new InterfaceUpdate(InterfaceUpdateAction.START_SYSTEM_TRAY_SYNC, null));
				}
				else if (syncingState.equals("in-sync")){
					Launcher.getEventBus().post(new InterfaceUpdate(InterfaceUpdateAction.STOP_SYSTEM_TRAY_SYNC, null));
				}
				break;
				
			case "start_syncing":
			    Launcher.getEventBus().post(new InterfaceUpdate(InterfaceUpdateAction.START_SYSTEM_TRAY_SYNC, null));
				break;
				
			case "stop_syncing":
			    Launcher.getEventBus().post(new InterfaceUpdate(InterfaceUpdateAction.STOP_SYSTEM_TRAY_SYNC, null));
				break;
		}
	}
}
