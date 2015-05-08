/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.eventbus.EventBus;

/**
 * The event bus wraps the Google EventBus service for the
 * daemon. It provides a publish/subscribe mechanism within a
 * single JVM.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
//TODO [medium] This class belongs in the 'util' package
public abstract class InternalEventBus {
	protected static final Logger logger = Logger.getLogger(InternalEventBus.class.getSimpleName());
	private static Map<Class<? extends InternalEventBus>, InternalEventBus> instances = new HashMap<>();
	
	protected EventBus eventBus;
	
	@SuppressWarnings("unchecked")
	protected static <T extends InternalEventBus> T getInstance(Class<T> eventBusClass) {
		T eventBusInstance = (T) instances.get(eventBusClass);
		
		if (eventBusInstance != null) {
			return eventBusInstance;
		}
		
		try {
			eventBusInstance = eventBusClass.newInstance();
			instances.put(eventBusClass, eventBusInstance);
		}
		catch (Exception e) {
			throw new RuntimeException("Cannot create event bus with class " + eventBusClass);
		}		
		
		return eventBusInstance;
	}
	
	protected InternalEventBus() {
		this.eventBus = new EventBus(this.getClass().getName());
		logger.log(Level.INFO, "Event bus: Created event bus " + this.getClass().getName());
	}
	
	public void register(Object object) {
		logger.log(Level.INFO, "Event bus '" + this.getClass().getSimpleName() + "': Registering " + object.getClass().getSimpleName() + " (" + object + ") ...");
		eventBus.register(object);
	}
	
	public void unregister(Object object) {
		logger.log(Level.INFO, "Event bus '" + this.getClass().getSimpleName() + "': Unregistering " + object.getClass().getSimpleName() + " (" + object + ") ...");
		eventBus.unregister(object);
	}	
	
	public void post(Object event) {
		logger.log(Level.INFO, "Event bus '" + this.getClass().getSimpleName() + "': Posting event " + event.getClass().getSimpleName() + " (" + event + ") ...");
		eventBus.post(event);
	}
}
