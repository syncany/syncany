package org.syncany.gui;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.syncany.gui.config.Profile;
import org.syncany.gui.messaging.ClientCommandFactory;
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
		this.shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				ClientCommandFactory.stopDaemon();
				ClientCommandFactory.stopWebSocketClient();
				System.exit(0);
			}
		});
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
			if (profil.isValid()) {
				ClientCommandFactory.handleWatch(profil.getFolder(), 120000, true);
			}
		}
	}

	@Subscribe
	public void updateInterface(Object event) {

	}

	public static String getClientIdentification() {
		return clientId;
	}
}