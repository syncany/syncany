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
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

/**
 * @author vincent
 *
 */
public class SWTUtil {

	public static boolean checkTextLength(Text text, int i) {
		String value = text.getText();
		boolean valid = true;
		if (value == null || !(value.length() > i)){
			text.setBackground(ApplicationResourcesManager.INVALID_TEXT_COLOR);
			valid = false;
		}
		else{
			text.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		}
		return valid;
	}

	public static boolean checkNumberBetween(Spinner spinner, int min, int max) {
		boolean valid = true;
		String value = spinner.getText();
		try{
			int port = Integer.parseInt(value);
			valid = port > min && port < max;
		}
		catch (Exception e){
			valid = false;
		}
		
		return valid;
	}

	public static boolean checkEquals(Text text, String match) {
		boolean valid = text.getText().equals(match);
		
		if (!valid){
			text.setBackground(ApplicationResourcesManager.INVALID_TEXT_COLOR);
		}
		else{
			text.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		}
		return valid;
	}
}
