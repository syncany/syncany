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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.swt.widgets.Shell;
import org.syncany.util.EnvironmentUtil;

/**
 * @author pheckel
 *
 */
public class TrayIconFactory {
	private Shell shell;

	public TrayIconFactory(Shell shell) {
		this.shell = shell;
	}

	public TrayIcon createTrayIcon() {
		if (EnvironmentUtil.isLinux() && isUnity()) {
			return new UnityTrayIcon();
		}
		else {
			return new DefaultTrayIcon(shell);
		}
	}

	private boolean isUnity() {
		ProcessBuilder processBuilder = new ProcessBuilder("/bin/ps", "--no-headers", "-C", "unity-panel-service");

		try {
			Process process = processBuilder.start();
			BufferedReader processInputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String ss = processInputReader.readLine();

			boolean isUnity = ss != null;

			process.destroy();
			processInputReader.close();

			return isUnity;
		}
		catch (IOException e) {
			throw new RuntimeException("Unable to determine Linux desktop environment.", e);
		}
	}
}
