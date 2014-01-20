package org.syncany.gui;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.syncany.gui.config.Profile;
import org.syncany.gui.messaging.ClientCommandFactory;
import org.syncany.gui.messaging.event.SyncyngEvent;
import org.syncany.gui.messaging.event.WatchUpdateEvent;
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
		if (shell != null && !shell.isDisposed()) {
			shell.dispose();
		}
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
		List<Profile> profiles = Launcher.applicationConfiguration.getProfiles();
		if (profiles == null) {
			return;
		}

		for (Profile profil : profiles) {
			File folderFile = new File(profil.getFolder());
			
			if (folderFile.exists()) {
				ClientCommandFactory.handleWatch(profil.getFolder(), 3000);
			}
		}
	}

	@Subscribe
	public void updateInterface(WatchUpdateEvent event) {
		Map<String, Map<String, String>> folders = event.getWatchUpdate();
		tray.updateFolders(folders);
	}

	@Subscribe
	public void updateInterface(SyncyngEvent event) {
		if (tray != null) {

			switch (event.getState()) {
			case SYNCING:
				tray.makeSystemTrayStartSync();
				break;

			case SYNCED:
				tray.makeSystemTrayStopSync();
				break;
			}
		}
	}

	public static String getClientIdentification() {
		return clientId;
	}
}