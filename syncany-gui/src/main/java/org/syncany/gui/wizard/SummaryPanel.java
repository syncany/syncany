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

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class CreateRepositorySummaryPanel extends WizardPanelComposite {
	private Label contentLabel;
	
	public CreateRepositorySummaryPanel(Composite parent, int style) {
		super(parent, style);
		initComposite();
	}
	
	private void initComposite(){
		setLayout(new GridLayout(1, false));
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		
		contentLabel = new Label(this, SWT.WRAP);
		contentLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		contentLabel.setText("New Label");
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<String, String> getUserSelection() {
		// TODO Auto-generated method stub
		return null;
	}
}
