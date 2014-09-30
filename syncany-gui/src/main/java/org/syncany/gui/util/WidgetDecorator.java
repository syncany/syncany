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


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.syncany.util.EnvironmentUtil;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class WidgetDecorator {
	public static final int VERTICAL_INDENT = 20;

	public static final int DEFAULT_BUTTON_WIDTH = 90;
	public static final int DEFAULT_BUTTON_HEIGHT = 30;
	
	public static final Color INVALID_TEXT_COLOR = SWTResourceManager.getColor(255, 218, 185);
	public static final Color RED = SWTResourceManager.getColor(SWT.COLOR_RED);
	public static final Color WHITE = SWTResourceManager.getColor(SWT.COLOR_WHITE);
	public static final Color BLACK = SWTResourceManager.getColor(SWT.COLOR_BLACK);
	public static final Color GRAY = SWTResourceManager.getColor(SWT.COLOR_GRAY);
	public static final Color COLOR_WIDGET = SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND);
	
	private static String FONT_NAME = "Segoe UI";
	private static int FONT_SIZE = EnvironmentUtil.isUnixLikeOperatingSystem() ? 12 : EnvironmentUtil.isWindows() ? 9 : 10;
	
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
	
	public static void normal(TreeItem...controls){
		for (TreeItem control : controls){
			control.setFont(FONT_NORMAL);
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
	private static void markAsInvalid(Control c){
		c.setBackground(INVALID_TEXT_COLOR);
	}
	
	private static void markAsValid(Control c){
		c.setBackground(WHITE);
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