package org.syncany.gui.main;

import java.net.URISyntaxException;
import java.util.Locale;
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
import org.syncany.gui.util.I18n;
import org.syncany.gui.util.SWTResourceManager;
import org.syncany.gui.websocket.WSClient;
import org.syncany.gui.wizard.StartDialog;

public class MainGUI {
	private static final Logger log = Logger.getLogger(MainGUI.class.getSimpleName());
	
	private static String clientIdentification = UUID.randomUUID().toString();

	private Shell shell;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		Locale.setDefault(Locale.ITALIAN);
		
		I18n.registerBundleName("i18n/messages");
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				log.info("Releasing SWT Resources");
				SWTResourceManager.dispose();
			}
		});
		
		log.info("Starting Graphical User Interface");
		
		MainGUI window = new MainGUI();
		window.open();
		
		WSClient client;
		try {
			client = new WSClient();
			client.startWebSocketConnection();
			ClientCommandFactory.setClient(client);
		}
		catch (URISyntaxException e) {
			log.warning("URISyntaxException :"+e);
		}
	}
	
	public void open() {
		shell = new Shell();
		installSystemTray();
		Display display = Display.getCurrent();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
	
	private void installSystemTray() {
		Display display = Display.getDefault();
		Tray tray = display.getSystemTray();
		
		if(tray != null) {
			TrayItem item = new TrayItem(tray, SWT.NONE);
			Image image = SWTResourceManager.getResizedImage("/images/tray/tray.png", 16, 16);
			item.setImage(image);

			final Menu menu = new Menu(shell, SWT.POP_UP);
			
			MenuItem connectItem = new MenuItem(menu, SWT.PUSH);
			connectItem.setText("Set-up a new connection");
			connectItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					StartDialog sd = new StartDialog(shell, SWT.APPLICATION_MODAL);
					sd.open();
				}
			});
			
			MenuItem quitMenu = new MenuItem(menu, SWT.PUSH);
			quitMenu.setText("Exit syncany");
			quitMenu.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					shell.dispose();
				}
			});

			item.addListener (SWT.MenuDetect, new Listener () {
				public void handleEvent (Event event) {
					menu.setVisible (true);
				}
			});
		}
	}
	
	/**
	 * @return the clientIdentification
	 */
	public static String getClientIdentification() {
		return clientIdentification;
	}
}