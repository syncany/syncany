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
package org.syncany.gui.tray;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.syncany.gui.Launcher;
import org.syncany.gui.messaging.ClientCommandFactory;
import org.syncany.gui.messaging.EventManager;
import org.syncany.gui.settings.SettingsDialog;
import org.syncany.gui.util.BrowserHelper;
import org.syncany.gui.wizard.WizardDialog;
import org.syncany.util.I18n;

/**
 * @author pheckel
 * @author Vincent Wiencek <vwiencek@gmail.com>
 */
public abstract class TrayIcon {
	private static int REFRESH_TIME = 1000;

	private Shell shell;
	private AtomicBoolean syncing = new AtomicBoolean(false);
	private AtomicBoolean paused = new AtomicBoolean(false);
	
	@SuppressWarnings("serial")
	protected Map<String, String> messages = new HashMap<String, String>(){{
		put("tray.menuitem.open", I18n.getString("tray.menuitem.open"));
		put("tray.menuitem.donate", I18n.getString("tray.menuitem.donate"));
		put("tray.menuitem.pause", I18n.getString("tray.menuitem.pause"));
		put("tray.menuitem.resume", I18n.getString("tray.menuitem.resume"));
		put("tray.menuitem.exit", I18n.getString("tray.menuitem.exit"));
		put("tray.menuitem.website", I18n.getString("tray.menuitem.website"));
	}};
	
	public enum TrayIcons {
		TRAY_NO_OVERLAY("/images/tray/tray.png"), 
		TRAY_IN_SYNC("/images/tray/tray-in-sync.png"), 
		TRAY_PAUSE_SYNC("/images/tray/tray-sync-pause.png"), 
		TRAY_SYNCING1("/images/tray/tray-syncing1.png"), 
		TRAY_SYNCING2("/images/tray/tray-syncing2.png"), 
		TRAY_SYNCING3("/images/tray/tray-syncing3.png"), 
		TRAY_SYNCING4("/images/tray/tray-syncing4.png"), 
		TRAY_SYNCING5("/images/tray/tray-syncing5.png"), 
		TRAY_SYNCING6("/images/tray/tray-syncing6.png"), 
		TRAY_UP_TO_DATE("/images/tray/tray-uptodate.png");

		private String fileName;

		TrayIcons(String filenName) {
			this.fileName = filenName;
		}

		public String getFileName() {
			return fileName;
		}

		public static TrayIcons get(int idx) {
			switch (idx + 1) {
			default:
			case 1:
				return TRAY_SYNCING1;
			case 2:
				return TRAY_SYNCING2;
			case 3:
				return TRAY_SYNCING3;
			case 4:
				return TRAY_SYNCING4;
			case 5:
				return TRAY_SYNCING5;
			case 6:
				return TRAY_SYNCING6;
			}
		}
	}

	public TrayIcon(Shell shell) {
		this.shell = shell;
		systemTrayAnimationThread.start();
	}

	public Shell getShell() {
		return shell;
	}

	protected void pause(File folder) {
		ClientCommandFactory.handlePauseWatch(folder.getAbsolutePath());
	}
	
	protected void resume(File folder) {
		ClientCommandFactory.handleResumeWatch(folder.getAbsolutePath());
	}
	
	protected void showFolder(File folder) {
		try {
			if (folder.exists() && folder.isDirectory()) {
				Desktop.getDesktop().open(folder);
			}
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	protected void showDonate() {
		BrowserHelper.browse("http://www.syncany.org/donate");
	}

	protected void showWebsite() {
		BrowserHelper.browse("http://www.syncany.org");
	}

	protected void quit() {
		Launcher.stopApplication();
	}

	public void makeSystemTrayStartSync() {
		syncing.set(true);
		paused.set(false);
	}

	public void makeSystemTrayStopSync() {
		syncing.set(false);
		paused.set(false);
		setTrayImage(TrayIcons.TRAY_IN_SYNC);
	}

	public void pauseSyncing() {
		paused.set(true);
		setTrayImage(TrayIcons.TRAY_PAUSE_SYNC);
	}

	public void resumeSyncing() {
		paused.set(false);
	}

	private Thread systemTrayAnimationThread = new Thread(new Runnable() {
		@Override
		public void run() {
			while (true) {
				while (paused.get() || !syncing.get()) {
					try {
						Thread.sleep(500);
					}
					catch (InterruptedException e) {
					}
				}

				int i = 0;

				while (syncing.get()) {
					try {
						setTrayImage(TrayIcons.get(i));
						i++;
						if (i == 6)
							i = 0;
						Thread.sleep(REFRESH_TIME);
					}
					catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				setTrayImage(TrayIcons.TRAY_IN_SYNC);
			}
		}
	});

	protected void showSettings() {
		shell.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				SettingsDialog wd = new SettingsDialog(shell, SWT.APPLICATION_MODAL);
				EventManager.register(wd);
				wd.setApplicationConfiguration(Launcher.applicationConfiguration);
				wd.open();
				EventManager.unregister(wd);
			}
		});
	}

	protected void showWizard() {
		shell.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				WizardDialog wd = new WizardDialog(getShell(), SWT.APPLICATION_MODAL);
				EventManager.register(wd);
				wd.open();
				EventManager.unregister(wd);
			}
		});
	}

	// Abstract methods
	protected abstract void setTrayImage(TrayIcons image);

	public abstract void updateFolders(Map<String, Map<String, String>> folders);

	public abstract void updateStatusText(String statusText);
}