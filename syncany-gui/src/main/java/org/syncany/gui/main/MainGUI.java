package org.syncany.gui.main;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.syncany.gui.command.ClientCommandFactory;
import org.syncany.gui.util.OS;
import org.syncany.gui.util.SWTResourceManager;
import org.syncany.gui.wizard.StartDialog;
import org.syncany.util.I18n;

public class MainGUI {
	private static final Logger log = Logger.getLogger(MainGUI.class.getSimpleName());
	private static String clientIdentification = UUID.randomUUID().toString();
	
	public static MainGUI window;
	private Display display = Display.getDefault();
	
	
	private Shell shell;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		if (OS.isMacOS()){
			System.setProperty("apple.awt.UIElement", "true");
		}
		start();
	}
	
	private static void start(){
		//Register messages bundles
		I18n.registerBundleName("i18n/messages");

		//Shutdown hook to release swt resources
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				log.info("Releasing SWT Resources");
				SWTResourceManager.dispose();
			}
		});
		
		ClientCommandFactory.list();
		
		log.info("Starting Graphical User Interface");
		
		window = new MainGUI();
		
		window.open();
	}
	
	public void open() {
		shell = new Shell();
		installSystemTray();
		
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	private List<MenuItem> items = new ArrayList<>();
	public void updateTray(Map<String, Map<String, String>> folders){
		for (MenuItem mi : items){
			mi.dispose();
		}

		items.clear();
		
		for (String key : folders.keySet()){
			MenuItem mi = new MenuItem(menu, SWT.PUSH);
			mi.setText(folders.get(key).get("folder")+" ["+ folders.get(key).get("status") + "]");
			items.add(mi);
		}
	}
	
	private TrayItem item;
	private Menu menu;
	
	private void installSystemTray() {
		Tray tray = Display.getDefault().getSystemTray();
		if(tray != null) {
			item = new TrayItem(tray, SWT.NONE);
			Image image = SWTResourceManager.getResizedImage("/images/tray/tray.png", 16, 16);
			item.setImage(image);

			menu = new Menu(shell, SWT.POP_UP);
			
			MenuItem connectItem = new MenuItem(menu, SWT.PUSH);
			connectItem.setText("Set-up a new connection");
			connectItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					StartDialog sd = new StartDialog(new HashMap<String, Object>(), shell, SWT.APPLICATION_MODAL);
					sd.open();
				}
			});
			
			MenuItem quitMenu = new MenuItem(menu, SWT.PUSH);
			quitMenu.setText("Exit syncany");
			quitMenu.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					shell.dispose();
					display.dispose();
					ClientCommandFactory.close();
				}
			});
			
			Listener showMenuListener = new Listener () {
				public void handleEvent (Event event) {
					menu.setVisible (true);
				}
			};
			
			item.addListener (SWT.MenuDetect, showMenuListener);
			item.addListener (SWT.Selection, showMenuListener);
		}
	}
	
	/**
	 * @return the clientIdentification
	 */
	public static String getClientIdentification() {
		return clientIdentification;
	}
}