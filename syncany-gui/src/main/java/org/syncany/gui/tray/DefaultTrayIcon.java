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

import java.io.File;
import java.util.HashMap;
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
import org.syncany.gui.SWTResourceManager;
import org.syncany.util.EnvironmentUtil;

/**
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class DefaultTrayIcon extends TrayIcon {
	private TrayItem trayItem;	
	private Menu menu;
	private MenuItem statusTextItem;

	@SuppressWarnings("serial")
	private Map<TrayIcons, Image> images = new HashMap<TrayIcons, Image>(){{
		for (TrayIcons ti : TrayIcons.values()){
			put(ti, SWTResourceManager.getImage(ti.getFileName(), false));
		}
	}};
	
	public DefaultTrayIcon(final Shell shell) {
		super(shell);
		
		buildTray();
		makeSystemTrayStartSync();
	}
	
	private void buildTray(){
		Tray tray = Display.getDefault().getSystemTray();
		
		if (tray != null) {
			trayItem = new TrayItem(tray, SWT.NONE);
			setTrayImage(TrayIcons.TRAY_IN_SYNC);
		
			buildMenuItems(null);			
			addMenuListeners();
		}
	}
	
	private void addMenuListeners() {
		Listener showMenuListener = new Listener() {
			public void handleEvent(Event event) {
				menu.setVisible(true);
			}
		};
		
		trayItem.addListener(SWT.MenuDetect, showMenuListener);
		
		if (!EnvironmentUtil.isLinux()) {
			// Tray icon popup menu positioning in Linux is off,
			// Disable it for now.
			
			trayItem.addListener(SWT.Selection, showMenuListener);
		}
	}

	private void buildMenuItems(final Map<String, Map<String, String>> folders) {
		if (menu != null) {
			clearMenuItems();
		}
		
		menu = new Menu(getShell(), SWT.POP_UP);
		
		statusTextItem = new MenuItem(menu, SWT.PUSH);
		statusTextItem.setText("All folders in sync");
		statusTextItem.setEnabled(false);
						
		new MenuItem(menu, SWT.SEPARATOR);
		
		MenuItem connectItem = new MenuItem(menu, SWT.PUSH);
		connectItem.setText("New sync folder");
		connectItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showWizard();
			}
		});
		
		MenuItem settingsItem = new MenuItem(menu, SWT.PUSH);
		settingsItem.setText("Preferences");
		settingsItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showSettings();
			}
		});
		
		if (folders != null && folders.size() > 0) {
			new MenuItem(menu, SWT.SEPARATOR);
			
			for (final String key : folders.keySet()) {
				File folder = new File(folders.get(key).get("folder"));
				String status = folders.get(key).get("status");
				
				MenuItem folderMenuItem = new MenuItem(menu, SWT.PUSH);
				folderMenuItem.setText(folder.getName() + " [" + status + "]");
				
				folderMenuItem.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						showFolder(new File(folders.get(key).get("folder")));						
					}
				});
			}			
		}
		
		new MenuItem(menu, SWT.SEPARATOR);
		
		MenuItem donateItem = new MenuItem(menu, SWT.PUSH);
		donateItem.setText("Donate");
		donateItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showDonate();
			}
		});
		
		MenuItem websiteItem = new MenuItem(menu, SWT.PUSH);
		websiteItem.setText("Website");
		websiteItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showWebsite();
			}
		});
		
		new MenuItem(menu, SWT.SEPARATOR);

		MenuItem quitMenu = new MenuItem(menu, SWT.PUSH);
		quitMenu.setText("Exit");
		quitMenu.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				quit();
			}
		});		
	}

	private void clearMenuItems() {
		if (menu != null) {
			while (menu.getItems().length > 0) {
				MenuItem item2 = menu.getItem(0);
				item2.dispose();
			}
		}
	}

	@Override
	public void updateFolders(final Map<String, Map<String, String>> folders) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				buildMenuItems(folders);
			}
		});
	}

	@Override
	public void updateStatusText(final String statusText) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				statusTextItem.setText(statusText);
			}
		});
	}

	@Override
	protected void setTrayImage(final TrayIcons image) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				trayItem.setImage(images.get(image));
			}
		});
	}
}
