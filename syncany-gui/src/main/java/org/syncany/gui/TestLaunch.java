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
package org.syncany.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.syncany.gui.wizard.WizardDialog;
import org.syncany.util.I18n;

/**
 * @author vwiencek
 *
 */
public class TestLaunch {
	public static void main(String[] args) {
		// Register messages bundles
		I18n.registerBundleName("i18n/messages");
		I18n.registerBundleFilter("plugin_messages*");

		Shell shell = new Shell();
		WizardDialog wd = new WizardDialog(shell, SWT.APPLICATION_MODAL);
		wd.open();
	}
}
