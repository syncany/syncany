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
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.syncany.util.EnvironmentUtil;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class WidgetDecorator {
	private static String FONT_NAME = "Segoe UI";
	private static int FONT_SIZE = EnvironmentUtil.isMacOS() ? 12 : EnvironmentUtil.isWindows() ? 9 : 10;
	
	private static Font FONT_NORMAL = SWTResourceManager.getFont(FONT_NAME, FONT_SIZE, SWT.NORMAL);
	private static Font FONT_BOLD = SWTResourceManager.getFont(FONT_NAME, FONT_SIZE, SWT.BOLD);
	
	public static void bold(Control...controls){
		for (Control control : controls){
			font(control, FONT_BOLD);
			if (control instanceof Text){
				enhanceFocus((Text)control);
			}
		}
	}
	
	public static void normal(Control...controls){
		for (Control control : controls){
			font(control, FONT_NORMAL);
			if (control instanceof Text){
				enhanceFocus((Text)control);
			}
		}
	}
	
	private static void font(Control control, Font font){
		control.setFont(font);
	}
	
	private static void enhanceFocus(Text control) {
		final Text text = (Text)control;
		text.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				text.selectAll();
			}			
		});
	}
}

