package org.syncany.gui;

import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.syncany.gui.messaging.InterfaceUpdate;

import com.google.common.eventbus.Subscribe;

public class MainGUI {
	private static final Logger log = Logger.getLogger(MainGUI.class.getSimpleName());
	private static String clientIdentification = UUID.randomUUID().toString();

	private Display display = Display.getDefault();
	private Shell shell;
	
	private SystemTrayManager sysTrayManager;
	
	public void open() {
		shell = new Shell();
		sysTrayManager = new SystemTrayManager(shell);

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	@Subscribe
	public void updateInterface(InterfaceUpdate update) {
		log.info("Update Interface Event : "+ update.getAction());
		
		switch (update.getAction()){
			case START_SYSTEM_TRAY_SYNC:
				sysTrayManager.makeSystemTrayStartSync();
				break;
			case STOP_SYSTEM_TRAY_SYNC:
				sysTrayManager.makeSystemTrayStopSync();
				break;
			case UPDATE_WATCHED_FOLDERS:
				sysTrayManager.updateTray(update.getData());
				break;
		}
	}

	/**
	 * @return the clientIdentification
	 */
	public static String getClientIdentification() {
		return clientIdentification;
	}
}