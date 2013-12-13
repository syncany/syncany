package org.syncany.gui;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Insets;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.UIManager;

import org.syncany.gui.panel.MainFrame;
import org.syncany.gui.util.ImageResources;
import org.syncany.gui.util.OS;
import org.syncany.gui.websocket.WSClient;

public class MainGUI {
	private static final Logger log = Logger.getLogger(MainGUI.class.getSimpleName());

	public static MainGUI instance;
	public static String clientIdentification = UUID.randomUUID().toString();
	
	private MainFrame mf;
	
	/**
	 * Launch the application.
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		try{ 
			UIManager.setLookAndFeel("com.jgoodies.looks.windows.WindowsLookAndFeel");
		}
		catch (Exception e){
			log.fine("unable to load jgoodies LnF");
		}
		
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					instance = new MainGUI();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		WSClient.startWebSocketConnection();
	}

	/**
	 * Create the application.
	 */
	public MainGUI() {
		mf = new MainFrame();
		mf.pack();
		installSystemTray();
	}
	
	/**
	 * @return the mf
	 */
	public MainFrame getMf() {
		return mf;
	}

	protected static Image createImage(String path, String description) {
		URL imageURL = MainGUI.class.getResource("/" + path);

		if (imageURL == null) {
			System.err.println("Resource not found: " + path);
			return null;
		}
		else {
			return (new ImageIcon(imageURL, description)).getImage();
		}
	}

	private void installSystemTray() {
		if (!SystemTray.isSupported()) {
			System.out.println("SystemTray is not supported");
			return;
		}

		final TrayIcon trayIcon = new TrayIcon(ImageResources.Image.TRAY.asBufferedImage());
		final SystemTray tray = SystemTray.getSystemTray();

		trayIcon.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent a) {
				showPanel(a.getX(), a.getY());
			}
		});
		
		try {
			tray.add(trayIcon);
		}
		catch (AWTException e) {
			System.out.println("TrayIcon could not be added.");
		}
	}

	protected void showPanel(int x, int y) {
		//If mainframe is already visible then hides it
		if (mf.isVisible()) {
			mf.setVisible(false);
			return;
		}
		
		int OFFSET = 10;
		
		int w = mf.getWidth();
		int h = mf.getHeight();
		
		//size of the screen
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		//height of the task bar
		Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(mf.getGraphicsConfiguration());
		int taskBarSize = scnMax.bottom;
		
		if (OS.isMacOS()){
			mf.setBounds(x - w/2,30, w, h);
		}
	
		//TODO [high] need to detect taskbar position (bottom, top, left, right)
		else if (OS.isWindows()){
			mf.setBounds(
				(int)screenSize.getWidth() - w - OFFSET,
				(int)screenSize.getHeight() - h - taskBarSize - OFFSET, 
				w, h);
		}
		else {
			mf.setBounds(x - w/2,30, w, h);
		}
		
		mf.setVisible(true);
	}
}