package org.syncany.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

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
import org.syncany.gui.messaging.InterfaceUpdate;
import org.syncany.gui.settings.SettingsDialog;
import org.syncany.gui.wizard.WizardDialog;
import org.syncany.util.EnvironmentUtil;

import com.google.common.eventbus.Subscribe;

public class MainGUI {
	private static final Logger log = Logger.getLogger(MainGUI.class.getSimpleName());
	private static String clientIdentification = UUID.randomUUID().toString();

	private Display display = Display.getDefault();
	private Shell shell;
	
	private Menu menu;
	private List<MenuItem> items = new ArrayList<>();
	
	private TrayItem item;

	public void open() {
		shell = new Shell();
		installSystemTray();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	private void updateTray(Map<String, Map<String, String>> folders) {
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

	@Subscribe
	public void updateInterface(InterfaceUpdate update) {
		log.info("Update Interface Event");
		updateTray(update.getData());
	}

	/**
	 * @return the clientIdentification
	 */
	public static String getClientIdentification() {
		return clientIdentification;
	}
}