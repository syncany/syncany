/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.gui.util;

import org.eclipse.swt.program.Program;
import org.syncany.util.EnvironmentUtil;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class BrowserHelper {

	public static void openFile(String filePath){
		Program.launch(filePath);
	}
	
	public static void browse(String url) {
		Runtime rt = Runtime.getRuntime();

		try {
			if (EnvironmentUtil.isWindows()) {
				// this doesn't support showing urls in the form of "page.html#nameLink"
				rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
			}
			//TODO add mac osx env detection
			else if (EnvironmentUtil.isOSX()) {
				rt.exec("open " + url);
			}
			else if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
				// Do a best guess on unix until we get a platform independent way
				// Build a list of browsers to try, in this order.
				String[] browsers = { "epiphany", "firefox", "mozilla", "konqueror", "netscape", "opera", "links", "lynx" };

				// Build a command string which looks like "browser1 "url" || browser2 "url" ||..."
				StringBuffer cmd = new StringBuffer();
				for (int i = 0; i < browsers.length; i++)
					cmd.append((i == 0 ? "" : " || ") + browsers[i] + " \"" + url + "\" ");

				rt.exec(new String[] { "sh", "-c", cmd.toString() });
			}
			else {
				return;
			}
		}
		catch (Exception e) {
			return;
		}
		return;
	}
}
