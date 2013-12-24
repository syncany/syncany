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
package org.syncany.gui.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.syncany.util.I18n;

/**
 * <code>DefaultDialog</code> subclasses SWT <code>Dialog</code> 
 * and implements basics usefull features
 * 
 * All SWT dialog for the application should subclass <code>DefaultDialog</code>
 * 
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public abstract class DefaultDialog extends Dialog {
	protected Shell shell;

	/**
	 * @param parent
	 * @param style
	 */
	public DefaultDialog(Shell parent, int style) {
		this(parent, style, null);
	}
	
	/**
	 * 
	 * @param parent <code>Shell</code> parent of this dialog
	 * @param style styles to be applied to the dialog
	 * @param title title of the dialog. By default. If title argument
	 * 		is null then, <code>DefaultDialog</code> looks for 
	 * 	<code>this.getCLass().getSimpleName() + ".dialog.title"</code> into i18n properties files
	 */
	public DefaultDialog(Shell parent, int style, String title) {
		super(parent, style);
		if (title == null){
			setText(I18n.getString(this.getClass().getSimpleName()+".dialog.title"));
		}
		else {
			setText(title);
		}
	}
	
	/**
	 * This method centers the dialog on the screen using
	 * <code>Display.getCurrent().getPrimaryManitor()</code>
	 */
	protected void centerOnScreen(){
		Monitor primary = Display.getCurrent().getPrimaryMonitor();
		Rectangle bounds = primary.getBounds ();
		Rectangle rect = shell.getBounds ();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		shell.setLocation (x, y);
	}

	protected void createContents() {
		shell = new Shell(getParent(), SWT.DIALOG_TRIM);
	}
}
