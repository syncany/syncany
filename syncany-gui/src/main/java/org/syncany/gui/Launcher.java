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
import java.util.logging.Logger;

import org.eclipse.swt.widgets.Display;
import org.syncany.config.Logging;
import org.syncany.daemon.Daemon;
import org.syncany.gui.config.ApplicationConfiguration;
import org.syncany.gui.config.ApplicationConfigurationTO;
import org.syncany.gui.config.ProxyController;
import org.syncany.gui.messaging.ClientCommandFactory;
import org.syncany.gui.messaging.WSClient;
import org.syncany.util.I18n;

import com.google.common.eventbus.EventBus;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class Launcher {
	private static final Logger log = Logger.getLogger(Launcher.class.getSimpleName());
	private static EventBus eventBus = new EventBus("syncany-gui");
	public static ApplicationConfiguration applicationConfiguration;
	
	public static Daemon daemon;
	
	static {
		Logging.init();
	}
	
	public static EventBus getEventBus() {
		return eventBus;
	}

	public static void main(String[] args) {
		startDaemon();
		startWebSocketClient();
		startGUI();	
	}

	private static void startDaemon(){
		daemon = new Daemon();
		daemon.start(true);
	}
	
	private static void startWebSocketClient() {
		try {
			new WSClient().startWebSocketConnection();
		}
		catch (Exception e) {
			throw new RuntimeException("Cannot start websocket client.", e);
		}
	}
	
	private static void startGUI(){
		Display.setAppName("Syncany");
		applicationConfiguration = null;
		try {
			ApplicationConfigurationTO acto = loadApplicationConfiguration();
			applicationConfiguration = ApplicationConfiguration.from(acto);
		}
		catch (Exception e) {
			log.severe("Unable to load application configuration File : "+e);
			return;
		}
		
		try{
			ProxyController.instance().initProxy(applicationConfiguration);
		}
		catch (Exception e){
			log.severe("Unable to initiate proxy");
		}
		
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

		ClientCommandFactory.startWebSocketClient();
		log.info("Starting Graphical User Interface");

		MainGUI window = new MainGUI();
		Launcher.getEventBus().register(window);
		window.open();
	}

	private static ApplicationConfigurationTO loadApplicationConfiguration() throws Exception {
		File saHome = new File(System.getProperty("user.home") + File.separator + ".syncany");
		File f = new File(saHome, "syncany-gui-config.xml");
		
		if (!f.exists()){ /** creates an empty ApplicationConfigurationTO file **/
			if (!saHome.exists()){
				saHome.mkdir();
			}
			ApplicationConfigurationTO.store(ApplicationConfigurationTO.getDefault(), f);
			log.info("Syncany gui configuration file created");
		}
		
		ApplicationConfigurationTO acto = ApplicationConfigurationTO.load(f);
		return acto;
	}
}
