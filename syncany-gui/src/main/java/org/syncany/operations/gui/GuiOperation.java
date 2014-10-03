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
package org.syncany.operations.gui;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.config.LocalEventBus;
import org.syncany.gui.Launcher;
import org.syncany.operations.Operation;
import org.syncany.operations.OperationResult;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 */
public class GuiOperation extends Operation {	
	private static final Logger logger = Logger.getLogger(GuiOperation.class.getSimpleName());	

	private LocalEventBus eventBus;

	public GuiOperation(){
		this(null);
	}
	
	public GuiOperation(Config config) {
		super(config);		
	}

	@Override
	public OperationResult execute() throws Exception {		
		logger.log(Level.INFO, "Starting GUI operation ...");
		
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					startGui();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.start();
		return null;
	}

	private void startGui() throws Exception {
		Launcher.main(null);
	}
}
