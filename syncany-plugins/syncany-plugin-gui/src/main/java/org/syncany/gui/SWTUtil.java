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
package org.syncany.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;

public class SWTUtil {
	private static void markAsInvalid(Control c){
		c.setBackground(ApplicationResourcesManager.INVALID_TEXT_COLOR);
	}
	private static void markAsValid(Control c){
		c.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
	}
	
	public static void markAs(boolean valid, Control c){
		if (valid) {
			markAsValid(c);
		}
		else {
			 markAsInvalid(c);
		}
	}
}