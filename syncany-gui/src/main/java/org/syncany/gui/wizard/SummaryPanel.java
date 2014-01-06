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

import java.util.HashMap;
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
public class SummaryPanel extends WizardPanelComposite {
	private Label repositoryType;
	
	public SummaryPanel(WizardDialog wizardParentDialog, Composite parent, int style) {
		super(wizardParentDialog, parent, style);
		initComposite();
	}
	
	private void initComposite(){
		setLayout(new GridLayout(2, false));
		
		Label repositoryTypeLanbl = new Label(this, SWT.NONE);
		repositoryTypeLanbl.setText("Repository Type");
		
		repositoryType = new Label(this, SWT.NONE);
		repositoryType.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		repositoryType.setText("New Label");
		
		Label lblNewLabel_2 = new Label(this, SWT.NONE);
		lblNewLabel_2.setText("Local Folder");
		
		Label lblNewLabel_3 = new Label(this, SWT.NONE);
		lblNewLabel_3.setText("New Label");
		
		Label lblNewLabel_4 = new Label(this, SWT.NONE);
		lblNewLabel_4.setText("Encryption Settings");
		
		Label lblNewLabel_5 = new Label(this, SWT.NONE);
		lblNewLabel_5.setText("New Label");
	}
	
	public void updateData(){
		Map<String, String> userInput = getParentWizardDialog().getUserInput();
		repositoryType.setText(userInput.get("repositoryType"));
	}
	
	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public Map<String, String> getUserSelection() {
		return new HashMap<String, String>();
	}

	@Override
	public boolean hasNextButton() {
		return false;
	}

	@Override
	public boolean hasPreviousButton() {
		return true;
	}

	@Override
	public boolean hasFinishButton() {
		return true;
	}
}
