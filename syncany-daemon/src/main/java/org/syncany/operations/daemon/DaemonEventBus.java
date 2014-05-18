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
package org.syncany.operations.daemon;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.eventbus.EventBus;

public class DaemonEventBus {
	private static final Logger logger = Logger.getLogger(DaemonEventBus.class.getSimpleName());
	private static DaemonEventBus instance;
	
	private EventBus eventBus;
	
	public static DaemonEventBus getInstance() {
		if (instance != null) {
			return instance;
		}
		
		instance = new DaemonEventBus();
		return instance;
	}
	
	private DaemonEventBus() {
		this.eventBus = new EventBus();
	}
	
	public void register(Object object) {
		logger.log(Level.INFO, "Event bus: Registering " + object + " (Class: " + object.getClass().getSimpleName() + ") ...");
		eventBus.register(object);
	}
	
	public void unregister(Object object) {
		logger.log(Level.INFO, "Event bus: Unregistering " + object + " (Class: " + object.getClass().getSimpleName() + ") ...");
		eventBus.unregister(object);
	}	
	
	public void post(Object event) {
		logger.log(Level.INFO, "Event bus: Posting event " + event + " (Class: " + event.getClass().getSimpleName() + ") ...");
		eventBus.post(event);
	}
}
