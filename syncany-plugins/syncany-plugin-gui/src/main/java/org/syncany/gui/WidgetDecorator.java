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


import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author vincent
 *
 */
public class WidgetDecorator {
	private static Font fontNormal = ApplicationResourcesManager.FONT_NORMAL;
	private static Font fontBold = ApplicationResourcesManager.FONT_BOLD;
	
	public static enum FontDecorator{
		NORMAL, BOLD;
	}
	
	public static void setFont(Control control, FontDecorator fontDecorator){
		switch (fontDecorator){
			case BOLD:
				control.setFont(fontBold);
				break;
			case NORMAL:
				control.setFont(fontNormal);
				break;
		}
	}
	
	public static void decorateText(final Text text, FontDecorator fontDecorator){
		setFont(text, fontDecorator);
		text.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				text.selectAll();
			}			
		});
	}

	public static void decorateLabel(Label label, FontDecorator fontDecorator) {
		setFont(label, fontDecorator);
	}
}

