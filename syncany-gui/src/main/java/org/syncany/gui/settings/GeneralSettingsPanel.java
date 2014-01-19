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
package org.syncany.gui.settings;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.syncany.gui.config.ApplicationConfiguration;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class GeneralSettingsPanel extends Composite {
	private ApplicationConfiguration configuration;
	
	/**
	 * @param parent
	 * @param style
	 */
	public GeneralSettingsPanel(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));
		
		Group grpSystem = new Group(this, SWT.NONE);
		grpSystem.setText("System");
		grpSystem.setLayout(new GridLayout(1, false));
		grpSystem.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		Button btnCheckButton = new Button(grpSystem, SWT.CHECK);
		btnCheckButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnCheckButton.setBounds(0, 0, 93, 16);
		btnCheckButton.setText("Launch Syncany at startup");
		
		Button btnCheckButton_1 = new Button(grpSystem, SWT.CHECK);
		btnCheckButton_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnCheckButton_1.setBounds(0, 0, 93, 16);
		btnCheckButton_1.setText("Display Toaster-like notifications");
		
		Group grpLanguage = new Group(this, SWT.NONE);
		grpLanguage.setText("Language");
		grpLanguage.setLayout(new GridLayout(1, false));
		grpLanguage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		Combo combo = new Combo(grpLanguage, SWT.NONE);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		combo.setBounds(0, 0, 91, 23);
		initComposite();
	}

	private void initComposite() {
		
	}

	public void setApplicationParameters(ApplicationConfiguration configuration){
		this.configuration = configuration;
	}
}
