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

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StandbyPreventor implements Runnable {
	private static final Logger logger = Logger.getLogger(StandbyPreventor.class.getSimpleName());
	private AtomicBoolean running = new AtomicBoolean(true);
	
	@Override
	public void run() {
		try {
			Robot robot = new Robot();
			
	    	while (running.get()) {	    		
	    		Point currentMousePosition = MouseInfo.getPointerInfo().getLocation();
	    		Point tempMousePosition = (currentMousePosition.x > 0) ? new Point(currentMousePosition.x-10, currentMousePosition.y) : new Point(currentMousePosition.x+10, currentMousePosition.y);
	    		
	    		logger.log(Level.INFO, "Standby prevention: Moving mouse 1px (and back): " + currentMousePosition);
	    		
	    		robot.mouseMove(tempMousePosition.x, tempMousePosition.y);
	    		robot.mouseMove(currentMousePosition.x, currentMousePosition.y);
	    		
	    		Thread.sleep(60000);
	    	}
	    	
	    	logger.log(Level.INFO, "Standby prevention stopped (gracefully).");
		}
		catch (InterruptedException e) {
			logger.log(Level.INFO, "Standby prevention stopped (interrupted).", e);
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Standby prevention failed.", e);
		}
	}
	
	public void stop() {
		running.set(false);
	}
}
