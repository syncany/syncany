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
 * The control server watches the daemon control file for changes and 
 * reacts on certain commands. 
 * 
 * <p>Although it is not a real socket, it can be seen as a cross-platform
 * unix-like socket. Due to the nature of the commands in the control 
 * file (shutdown/reload), a normal TCP socket is not possible.
 * 
 * <p>The central method is {@link #enterLoop()}: This method tails file changes
 * in the daemon control file in the currently active thread. It does not
 * fork a new thread. It <b>blocks</b> and waits for commands until 
 * <b>shutdown</b> is received.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class ControlServer implements TailerListener {	
	private static final Logger logger = Logger.getLogger(ControlServer.class.getSimpleName());
	private static final String CONTROL_FILE = "daemon.ctrl";

	public enum ControlCommand {
		SHUTDOWN, RELOAD
	}
	
	private File controlFile;
	private Tailer controlFileTailer;
	private LocalEventBus eventBus;

	public ControlServer() {
		this.controlFile = new File(UserConfig.getUserConfigDir(), CONTROL_FILE);
		this.controlFileTailer = new Tailer(controlFile, this, 1000, true);
		this.eventBus = LocalEventBus.getInstance();		
	}

	public void enterLoop() throws IOException, ServiceAlreadyStartedException {
		File userAppDir = UserConfig.getUserConfigDir();
		userAppDir.mkdirs();
		
		controlFile.delete();
		controlFile.createNewFile();		
		controlFile.deleteOnExit();	
		
		logger.log(Level.INFO, "Monitoring control file for commands at " + controlFile + " ...");
		logger.log(Level.INFO, "   (Note: This is a blocking operation. The 'main' thread is now blocked until '" + ControlCommand.SHUTDOWN + "' is received.)");
		
		controlFileTailer.run(); // This blocks!
	}	

	/**
	 * Functions that handle tailing the control file.
	 */
	@Override
	public void fileNotFound() {
		logger.log(Level.SEVERE, "Control file not found. FATAL. EXITING.");
		throw new RuntimeException("Control file not found. FATAL. EXITING.");
	}
	
	@Override
	public void handle(String command) {
		try {
			ControlCommand controlCommand = ControlCommand.valueOf(command.trim().toUpperCase());
			
			switch (controlCommand) {
			case SHUTDOWN:
				logger.log(Level.INFO, "Control file: Received shutdown command. Shutting down.");

				eventBus.post(controlCommand);
				controlFileTailer.stop();
				break;
				
			case RELOAD:
				logger.log(Level.INFO, "Control file: Received reload command. Reloading config ...");

				eventBus.post(controlCommand);
				break;
				
			default:
				throw new RuntimeException("This command should have been handled.");
			}
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Control file: Ignoring unknown command: " + command, e);
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
