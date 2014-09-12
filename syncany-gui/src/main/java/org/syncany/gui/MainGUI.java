package org.syncany.gui;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.syncany.config.UserConfig;
import org.syncany.gui.command.GUIClient;
import org.syncany.gui.tray.TrayIcon;
import org.syncany.gui.tray.TrayIconFactory;
import org.syncany.operations.daemon.DaemonOperation;
import org.syncany.operations.daemon.LocalEventBus;
import org.syncany.operations.daemon.messages.ListWatchesManagementRequest;
import org.syncany.operations.daemon.messages.ListWatchesManagementResponse;
import org.syncany.operations.daemon.messages.api.Request;
import org.syncany.util.PidFileUtil;

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
				System.exit(0);
			}
		});
		this.tray = new TrayIconFactory().createTrayIcon(shell);

		LocalEventBus.getInstance().register(this);
	}

	public void open() {
		Display display = Display.getDefault();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	public void restoreWatchedFolders() {
		logger.info("Restoring watched folders");
		
		File daemonPidFile = new File(UserConfig.getUserConfigDir(), DaemonOperation.PID_FILE);
		boolean daemonRunning = PidFileUtil.isProcessRunning(daemonPidFile);

		if (daemonRunning) {
			GUIClient gc = new GUIClient();
			Request req = new ListWatchesManagementRequest();
			req.setId(Math.abs(new Random().nextInt()));
			ListWatchesManagementResponse response = (ListWatchesManagementResponse) gc.runCommand(req);
			
			Map<String, Map<String, String>> folders = new HashMap<>();
			
			for (File f : response.getWatches()) {
				Map<String, String> data = new HashMap<>();
				data.put("folder", f.getAbsolutePath());
				data.put("status", "status");
				folders.put(UUID.randomUUID().toString(), data);
			}
		
			tray.updateFolders(folders);
		}
	}

	@Subscribe
	public void updateInterface(Object event) {
		
	}

	public static String getClientIdentification() {
		return clientId;
	}
}