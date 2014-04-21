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

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.syncany.Client;

/**
 * @author pheckel
 *
 */
public class DaemonControlServer implements TailerListener {	
	private File controlFile;
	private Tailer controlFileTailer;
	private ShutdownListener shutdownListener;

	public DaemonControlServer(ShutdownListener shutdownListener) {
		this.controlFile = new File(Client.getUserAppDir(), "control");
		this.controlFileTailer = new Tailer(controlFile, this, 1000, true);
		this.shutdownListener = shutdownListener;
	}

	public void enterLoop() throws IOException {
		File userAppDir = Client.getUserAppDir();
		userAppDir.mkdirs();
				
		if (!controlFile.exists()) {
			controlFile.createNewFile();
		}
		
		controlFile.deleteOnExit();		
		controlFileTailer.run(); 
	}	

	@Override
	public void init(Tailer tailer) {
		// Nothing
	}

	@Override
	public void fileNotFound() {
		System.out.println("file not found");
	}

	@Override
	public void fileRotated() {
		System.out.println("file rotated");
	}

	@Override
	public void handle(String line) {
		System.out.println(line);
		
		switch (line) {
		case "stop":
			System.out.println("Stopping");
			shutdownListener.onDaemonShutdown();
			controlFileTailer.stop();
			break;
		}
	}

	@Override
	public void handle(Exception e) {
		throw new RuntimeException(e);
	}

}
