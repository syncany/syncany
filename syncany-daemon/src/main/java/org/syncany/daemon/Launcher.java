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
package org.syncany.daemon;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.syncany.daemon.exception.ServiceAlreadyStartedException;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class Launcher implements TailerListener {
	private Tailer tailer;
	
	public static void main(String[] args) throws ServiceAlreadyStartedException, InterruptedException {		
		new Launcher().start();		
	}
	
	public void start() throws InterruptedException {
		startDaemon();
		startTailer();	
	}
	
	private void startDaemon() {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("lockPort", 3338);

		try {
			ServiceManager.startService("daemon1", "org.syncany.daemon.Daemon",params);
		}
		catch (Exception e) {
			e.printStackTrace();
		}		
	}

	private void startTailer() {
		tailer = new Tailer(new File("/tmp/commands"), this, 1000, true);		
		tailer.run(); // This blocks! 
	}

	@Override
	public void init(Tailer tailer) {
		// TODO Auto-generated method stub
		
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
			ServiceManager.stopService("daemon1");
			tailer.stop();
			break;
		}
	}

	@Override
	public void handle(Exception ex) {
		ex.printStackTrace();
	}
}
