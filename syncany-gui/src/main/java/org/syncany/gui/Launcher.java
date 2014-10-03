/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.gui;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.eclipse.swt.widgets.Display;
import org.syncany.config.Logging;
import org.syncany.config.UserConfig;
import org.syncany.gui.util.I18n;
import org.syncany.gui.util.SWTResourceManager;
import org.syncany.operations.daemon.DaemonOperation;
import org.syncany.util.EnvironmentUtil;
import org.syncany.util.PidFileUtil;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class Launcher {
	private static final Logger log = Logger.getLogger(Launcher.class.getSimpleName());
	
	public static MainGUI window;

	static {
		Logging.init();
		//Logging.disableLogging();
	}

	public static void main(String[] args) {
		try {
			startDeamon();
		}
		catch (IOException | InterruptedException e) {
			log.warning("Unable to start daemon");
		}

		startApplication();
	}

	private static void startApplication() {
		UserConfig.init();
		startGUI();
	}

	public static void stopApplication() {
		try {
			shutDownDaemon();
		}
		catch (IOException e) {
			log.warning("Unable to stop daemon: " + e);
		}
		catch (InterruptedException e) {
			log.warning("Unable to stop daemon: " + e);
		}
		stopGUI();
		System.exit(0);
	}

	private static void stopGUI() {
		window.dispose();
	}

	public static void startDeamon() throws IOException, InterruptedException {
		File daemonPidFile = new File(UserConfig.getUserConfigDir(), DaemonOperation.PID_FILE);

		boolean daemonRunning = PidFileUtil.isProcessRunning(daemonPidFile);

		if (!daemonRunning){
			String executable = null;
			if (EnvironmentUtil.isMaxOsX()){
				executable = "/usr/local/bin/sy";
			}
			else if (EnvironmentUtil.isWindows()){
				executable = "c:\\windows\\sy.bat";
			}
			Process daemonProceee = new ProcessBuilder(executable, "daemon", "start").start();
			daemonProceee.waitFor();
		}
	}
	
	public static void shutDownDaemon() throws IOException, InterruptedException {
		File daemonPidFile = new File(UserConfig.getUserConfigDir(), DaemonOperation.PID_FILE);

		boolean daemonRunning = PidFileUtil.isProcessRunning(daemonPidFile);

		if (daemonRunning){
			String executable = null;
			if (EnvironmentUtil.isMaxOsX()){
				executable = "/usr/local/bin/sy";
			}
			else if (EnvironmentUtil.isWindows()){
				executable = "c:\\windows\\sy.bat";
			}
			Process daemonProceee = new ProcessBuilder(executable, "daemon", "stop").start();
			daemonProceee.waitFor();
		}
	}
	
	private static void startGUI() {
		Display.setAppName("Syncany");
		Display.setAppVersion("1.0");
		
		// Register messages bundles
		I18n.registerBundleName("i18n/messages");
		I18n.registerBundleFilter("plugin_messages*");

		// Shutdown hook to release swt resources
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				log.info("Releasing SWT Resources");
				SWTResourceManager.dispose();
			}
		});

		log.info("Starting Graphical User Interface");

		window = new MainGUI();
		window.open();
	}
}
