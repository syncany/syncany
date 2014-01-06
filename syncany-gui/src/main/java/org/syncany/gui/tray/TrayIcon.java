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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.syncany.gui.Launcher;
import org.syncany.gui.messaging.ClientCommandFactory;
import org.syncany.gui.settings.SettingsDialog;

/**
 * @author pheckel
 *
 */
public abstract class TrayIcon {
	private static final Logger logger = Logger.getLogger(TrayIcon.class.getSimpleName());
	
	private Shell shell;

	public TrayIcon(Shell shell) {
		this.shell = shell;
	}
	
	public abstract void updateFolders(Map<String, Map<String, String>> folders);
	public abstract void updateStatusText(String statusText);
	public abstract void makeSystemTrayStartSync();
	public abstract void makeSystemTrayStopSync();
	
	private void showWebSite(String url){
		if (Desktop.isDesktopSupported()){
			try {
				Desktop.getDesktop().browse(new URI(url));
			}
			catch (IOException e) {
				logger.warning("IOException " + e);
			}
			catch (URISyntaxException e) {
				logger.warning("URISyntaxException " + e);
			}
		}
	}
	
	protected void showDonate(){
		showWebSite("http://www.syncany.org/donate");
	}
	
	protected void showWebsite(){
		showWebSite("http://www.syncany.org");
	}
	
	protected void quit(){
		ClientCommandFactory.closeWebSocketClient();
		Launcher.daemon.shutdown();
		shell.dispose();
	}
	
	protected void showSettings(){
		SettingsDialog wd = new SettingsDialog(shell, SWT.APPLICATION_MODAL);
		wd.open();
	}
}
