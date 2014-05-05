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

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.syncany.config.UserConfig;

/**
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class DaemonControlServer implements TailerListener {	
	private static final Logger logger = Logger.getLogger(DaemonControlServer.class.getSimpleName());
	private static final String COMMAND_SHUTDOWN = "shutdown";
	
	private File controlFile;
	private Tailer controlFileTailer;
	private ShutdownListener shutdownListener;

	public DaemonControlServer(ShutdownListener shutdownListener) {
		this.controlFile = new File(UserConfig.getUserConfigDir(), "control");
		this.controlFileTailer = new Tailer(controlFile, this, 1000, true);
		this.shutdownListener = shutdownListener;
	}

	public void enterLoop() throws IOException {
		File userAppDir = UserConfig.getUserConfigDir();
		userAppDir.mkdirs();
				
		if (!controlFile.exists()) {
			controlFile.createNewFile();
		}
		
		controlFile.deleteOnExit();		

		logger.log(Level.INFO, "Monitoring control file for commands at " + controlFile + " ...");
		logger.log(Level.INFO, "   (Note: This is a blocking operation. The 'main' thread is now blocked until '" + COMMAND_SHUTDOWN + "' is received.)");
		
		controlFileTailer.run(); 
	}	

	@Override
	public void fileNotFound() {
		logger.log(Level.SEVERE, "Control file not found. FATAL. EXITING.");
		throw new RuntimeException("Control file not found. FATAL. EXITING.");
	}
	
	@Override
	public void handle(String command) {
		switch (command) {
		case COMMAND_SHUTDOWN:
			logger.log(Level.INFO, "Control file: Received shutdown command. Shutting down.");

			shutdownListener.onDaemonShutdown();
			controlFileTailer.stop();
			break;
			
		default:
			logger.log(Level.WARNING, "Control file: Ignoring unknown command: " + command);
		}
	}

	@Override
	public void handle(Exception e) {
		logger.log(Level.SEVERE, "Control file tailer exception received. FATAL. EXITING.", e);
		throw new RuntimeException("Control file tailer exception received. FATAL. EXITING.", e);
	}
	
	@Override
	public void init(Tailer tailer) {
		// Don't care
	}
	
	@Override
	public void fileRotated() {
		// Don't care
	}

}
