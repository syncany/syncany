package org.syncany.gui;

import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.syncany.config.LocalEventBus;
import org.syncany.gui.controller.SyncController;
import org.syncany.gui.messaging.websocket.WebSocket;
import org.syncany.gui.tray.TrayIcon;
import org.syncany.gui.tray.TrayIconFactory;

public class MainGUI {
	private static final Logger logger = Logger.getLogger(MainGUI.class.getSimpleName());

	/**
	 * Unique client identification
	 * Used to allow identification in communications
	 * between client and daemon server
	 **/
	private static String clientId = UUID.randomUUID().toString();
	
	private WebSocket client;
	
	private Shell shell;
	private TrayIcon tray;

	public void dispose() {
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
	}

	public MainGUI() {
		this.shell = new Shell();
		this.shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				System.exit(0);
			}
		});
		this.tray = new TrayIconFactory().createTrayIcon(shell);

		LocalEventBus.getInstance().register(this);

		// Websocket connection
		client = new WebSocket(); 
		client.init();
		
		// REST Call to initialise watched folders
		SyncController.getInstance().restoreWatchedFolders();
	}

	public void open() {
		Display display = Display.getDefault();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	public static String getClientIdentification() {
		return clientId;
	}
	
	/**
	 * @return the tray
	 */
	public TrayIcon getTray() {
		return tray;
	}
}