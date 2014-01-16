package org.syncany.gui;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.syncany.gui.messaging.ClientCommandFactory;
import org.syncany.gui.messaging.InterfaceUpdateEvent;
import org.syncany.gui.tray.TrayIcon;
import org.syncany.gui.tray.TrayIconFactory;

import com.google.common.eventbus.Subscribe;

public class MainGUI {
	private static final Logger logger = Logger.getLogger(MainGUI.class.getSimpleName());

	/**
	 * Unique client identification
	 * Used to allow identification in communications
	 * between client and daemon server
	 **/
	private static String clientId = UUID.randomUUID().toString();

	private Shell shell;
	private TrayIcon tray;

	public void dispose() {
		shell.dispose();
	}

	public MainGUI() {
		this.shell = new Shell();
		this.tray = new TrayIconFactory().createTrayIcon(shell);
	}

	public void open() {
		Display display = Display.getDefault();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	public static void restoreWatchedFolders() {
		logger.info("Restoring watched folders");
		List<String> wf = Launcher.applicationConfiguration.getWatchedFolders();
		if (wf == null) {
			return;
		}

		for (String folder : wf) {
			ClientCommandFactory.handleWatch(folder, 3000);
		}
	}

	@Subscribe
	public void updateInterface(InterfaceUpdateEvent update) {
		if (tray != null) {
			logger.info("Update Interface Event : " + update.getAction());
			
			switch (update.getAction()) {
			case START_SYSTEM_TRAY_SYNC:
				tray.makeSystemTrayStartSync();
				break;
				
			case STOP_SYSTEM_TRAY_SYNC:
				tray.makeSystemTrayStopSync();
				
				break;
			case UPDATE_WATCHED_FOLDERS:
				Map<String, ?> data = update.getData();
				Map<String, Map<String, String>> folders = (Map<String, Map<String, String>>) data; // TODO [low] Can this be done differently?
				
				tray.updateFolders(folders);
				break;
				
			default:
				logger.log(Level.WARNING, "Action NOT supported: " + update.getAction());
			}
		}
	}

	public static String getClientIdentification() {
		return clientId;
	}
}