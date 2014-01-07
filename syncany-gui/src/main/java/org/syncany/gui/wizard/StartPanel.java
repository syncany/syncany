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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.syncany.gui.ApplicationResourcesManager;
import org.syncany.gui.SyncanyCommandParameters;
import org.syncany.gui.UserInput;
import org.syncany.util.I18n;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class StartPanel extends WizardPanelComposite {
	//Widgets
	private Button createStorageRadio;
	private Button connectStorageRadio;
	
	//Constructor
	public StartPanel(WizardDialog wizardParentDialog, Composite composite, int style) {
		super(wizardParentDialog, composite, SWT.NONE);
		initComposite();
	}
	
	private void initComposite(){
		Font fontNormal = ApplicationResourcesManager.FONT_NORMAL;
		Font fontBold = ApplicationResourcesManager.FONT_BOLD;
		
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(1, false);
		gl_composite.marginRight = 30;
		setLayout(gl_composite);
		
		Label introductionTitleText = new Label(this, SWT.WRAP);
		introductionTitleText.setFont(fontBold);
		introductionTitleText.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		introductionTitleText.setText(I18n.getString("dialog.start.introductionText.title"));
		
		Label introductionText = new Label(this, SWT.WRAP);
		introductionText.setFont(fontNormal);
		introductionText.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 1));
		introductionText.setText(I18n.getString("dialog.start.introductionText"));

		createStorageRadio = new Button(this, SWT.RADIO);
		createStorageRadio.setFont(fontNormal);
		GridData gd_createStorageRadio = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_createStorageRadio.verticalIndent = 20;
		gd_createStorageRadio.horizontalIndent = 30;
		gd_createStorageRadio.heightHint = 30;
		createStorageRadio.setLayoutData(gd_createStorageRadio);
		createStorageRadio.setBounds(0, 0, 90, 16);
		createStorageRadio.setText(I18n.getString("dialog.start.option.createOnlineStorage"));
		createStorageRadio.setSelection(true);
		
		Label createText = new Label(this, SWT.WRAP);
		GridData gd_createText = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		gd_createText.horizontalIndent = 30;
		createText.setLayoutData(gd_createText);
		createText.setFont(fontNormal);
		createText.setText(I18n.getString("dialog.start.option.createOnlineStorage.helpText"));
		
		connectStorageRadio = new Button(this, SWT.RADIO);
		GridData gd_connectStorageRadio = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_connectStorageRadio.verticalIndent = 20;
		gd_connectStorageRadio.horizontalIndent = 30;
		gd_connectStorageRadio.heightHint = 30;
		connectStorageRadio.setLayoutData(gd_connectStorageRadio);
		connectStorageRadio.setBounds(0, 0, 90, 16);
		connectStorageRadio.setFont(fontNormal);
		connectStorageRadio.setText(I18n.getString("dialog.start.option.connectExisting"));
		
		Label connectText = new Label(this, SWT.WRAP);
		GridData gd_connectText = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		gd_connectText.horizontalIndent = 30;
		connectText.setLayoutData(gd_connectText);
		connectText.setFont(fontNormal);
		connectText.setText(I18n.getString("dialog.start.option.connectExisting.helpText"));
	}

	@Override
	public boolean isValid() {
		return createStorageRadio.getSelection() || connectStorageRadio.getSelection();
	}

	@Override
	public UserInput getUserSelection() {
		UserInput userInput = new UserInput();
		if (createStorageRadio.getSelection()){
			userInput.put(SyncanyCommandParameters.COMMAND_ACTION, "create");
		}
		else{
			userInput.put(SyncanyCommandParameters.COMMAND_ACTION, "connect");
		}
		return userInput;
	}

	@Override
	public boolean hasNextButton() {
		return true;
	}

	@Override
	public boolean hasPreviousButton() {
		return false;
	}

	@Override
	public boolean hasFinishButton() {
		return false;
	}

	@Override
	public void updateData() {
		// TODO Auto-generated method stub
		
	}
}
