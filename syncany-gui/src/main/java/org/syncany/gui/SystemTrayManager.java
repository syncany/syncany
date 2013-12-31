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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.syncany.gui.messaging.ClientCommandFactory;
import org.syncany.gui.settings.SettingsDialog;
import org.syncany.gui.wizard.WizardDialog;
import org.syncany.util.EnvironmentUtil;

/**
 * @author vwiencek
 *
 */
public class SystemTrayManager {
	private Menu menu;
	private List<MenuItem> items = new ArrayList<>();
	
	private TrayItem item;
	private Shell shell;

	private AtomicBoolean syncing = new AtomicBoolean(false);
	private AtomicBoolean running = new AtomicBoolean(false);
	
	private String[] animation = new String[]{
		"/images/tray/tray-syncing1.png",
		"/images/tray/tray-syncing2.png",
		"/images/tray/tray-syncing3.png",
		"/images/tray/tray-syncing4.png",
		"/images/tray/tray-syncing5.png",
		"/images/tray/tray-syncing6.png",
	};
	
	public SystemTrayManager(Shell shell){
		this.shell = shell;
		installSystemTray();
	}
	
	private void installSystemTray() {
		Tray tray = Display.getDefault().getSystemTray();
		
		if (tray != null) {
			item = new TrayItem(tray, SWT.NONE);

			if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
				Image image = SWTResourceManager.getImage("/images/tray/tray.png");
				item.setImage(image);
			}
			else {
				Image image = SWTResourceManager.getResizedImage("/images/tray/tray.png", 16, 16);
				item.setImage(image);
			}

			menu = new Menu(shell, SWT.POP_UP);

			MenuItem connectItem = new MenuItem(menu, SWT.PUSH);
			connectItem.setText("New connection");
			connectItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					WizardDialog wd = new WizardDialog(shell, SWT.APPLICATION_MODAL);
					wd.open();
				}
			});
			
			MenuItem settingsItem = new MenuItem(menu, SWT.PUSH);
			settingsItem.setText("Settings");
			settingsItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					SettingsDialog wd = new SettingsDialog(shell, SWT.APPLICATION_MODAL);
					wd.open();
				}
			});

			MenuItem quitMenu = new MenuItem(menu, SWT.PUSH);
			quitMenu.setText("Exit");
			quitMenu.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					shell.dispose();
					shell.getDisplay().dispose(); // TODO not sure if required
					
					ClientCommandFactory.closeWebSocketClient();
					Launcher.daemon.shutdown();
				}
			});

			Listener showMenuListener = new Listener() {
				public void handleEvent(Event event) {
					menu.setVisible(true);
				}
			};

			item.addListener(SWT.MenuDetect, showMenuListener);
			
			if (!EnvironmentUtil.isLinux()) {
				// Tray icon popup menu positioning in Linux is off,
				// Disable it for now.
				
				item.addListener(SWT.Selection, showMenuListener);
			}
			
			start();
		}
	}
	
	private Thread systemTrayAnimationThread = new Thread(new Runnable() {
		@Override
		public void run() {
			while (true){
				int i = 0;
				
				while (syncing.get()){
					try {
						final int idx = i;
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								item.setImage(SWTResourceManager.getImage(animation[idx]));
							}
						});
						i++;
						if (i == 6) i = 0;
						Thread.sleep(500);
					}
					catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						item.setImage(SWTResourceManager.getImage("/images/tray/tray-in-sync.png"));
					}
				});
				
				running.set(false);
				
				while (!running.get()){

				}
			}
		}
	});
	
	public void makeSystemTrayStartSync(){
		syncing.set(true);
		running.set(true);
	}
	
	public void makeSystemTrayStopSync(){
		syncing.set(false);
	}
	
	public void updateTray(Map<String, Map<String, String>> folders) {
		for (MenuItem mi : items) {
			mi.dispose();
		}

		items.clear();

		for (String key : folders.keySet()) {
			MenuItem mi = new MenuItem(menu, SWT.PUSH);
			mi.setText(folders.get(key).get("folder") + " [" + folders.get(key).get("status") + "]");
			items.add(mi);
		}
	}

	private void start() {
		systemTrayAnimationThread.start();
	}
}
