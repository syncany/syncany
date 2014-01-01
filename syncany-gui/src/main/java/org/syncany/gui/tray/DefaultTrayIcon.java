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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.syncany.gui.Launcher;
import org.syncany.gui.SWTResourceManager;
import org.syncany.gui.messaging.ClientCommandFactory;
import org.syncany.gui.settings.SettingsDialog;
import org.syncany.gui.wizard.WizardDialog;
import org.syncany.util.EnvironmentUtil;

/**
 * @author pheckel
 *
 */
public class DefaultTrayIcon implements TrayIcon {
	private static final String TRAY_ICON_IMAGE_RESOURCE = "/images/tray/tray.png";
	private TrayItem item;
	
	private Menu menu;
	private List<MenuItem> items = new ArrayList<>();
	private MenuItem statusTextItem;
	
	private Display display = Display.getDefault();
	
	public DefaultTrayIcon(final Shell shell) {
		Tray tray = Display.getDefault().getSystemTray();
		
		if (tray != null) {
			item = new TrayItem(tray, SWT.NONE);

			Image image = (EnvironmentUtil.isUnixLikeOperatingSystem()) ? SWTResourceManager.getImage(TRAY_ICON_IMAGE_RESOURCE) : SWTResourceManager.getResizedImage(TRAY_ICON_IMAGE_RESOURCE, 16, 16);
			item.setImage(image);

			menu = new Menu(shell, SWT.POP_UP);
						
			statusTextItem = new MenuItem(menu, SWT.PUSH);
			statusTextItem.setText("All folders in sync");
			statusTextItem.setEnabled(false);
			
			new MenuItem(menu, SWT.SEPARATOR);
			
			MenuItem connectItem = new MenuItem(menu, SWT.PUSH);
			connectItem.setText("New sync folder");
			connectItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					WizardDialog wd = new WizardDialog(shell, SWT.APPLICATION_MODAL);
					wd.open();
				}
			});
			
			MenuItem settingsItem = new MenuItem(menu, SWT.PUSH);
			settingsItem.setText("Preferences");
			settingsItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					SettingsDialog wd = new SettingsDialog(shell, SWT.APPLICATION_MODAL);
					wd.open();
				}
			});
			
			MenuItem donateItem = new MenuItem(menu, SWT.PUSH);
			donateItem.setText("Donate");
			donateItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					System.out.println("donate");
				}
			});
			
			MenuItem websiteItem = new MenuItem(menu, SWT.PUSH);
			websiteItem.setText("Website");
			websiteItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					System.out.println("website");
				}
			});

			MenuItem quitMenu = new MenuItem(menu, SWT.PUSH);
			quitMenu.setText("Exit");
			quitMenu.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					shell.dispose();
					display.dispose();
					
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
		}
	}

	@Override
	public void updateFolders(Map<String, Map<String, String>> folders) {
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

	@Override
	public void updateStatusText(String statusText) {
		statusTextItem.setText(statusText);		
	}
}
