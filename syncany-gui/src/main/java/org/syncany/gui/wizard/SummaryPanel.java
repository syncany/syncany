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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.syncany.gui.ApplicationResourcesManager;
import org.syncany.gui.SyncanyCommandParameters;
import org.syncany.gui.UserInput;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class SummaryPanel extends WizardPanelComposite {
	private Label repositoryType;
	private Label localFolder;
	private Label encryptionType;
	private Label lblNewLabel;
	
	public SummaryPanel(WizardDialog wizardParentDialog, Composite parent, int style) {
		super(wizardParentDialog, parent, style);
		initComposite();
	}
	
	private void initComposite(){
		Font fontNormal = ApplicationResourcesManager.FONT_NORMAL;
		Font fontBold = ApplicationResourcesManager.FONT_BOLD;
		
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.horizontalSpacing = 8;
		gridLayout.verticalSpacing = 8;
		setLayout(gridLayout);
		
		lblNewLabel = new Label(this, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		lblNewLabel.setText("New Label");
		
		Label repositoryTypeLabel = new Label(this, SWT.NONE);
		GridData gd_repositoryTypeLabel = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_repositoryTypeLabel.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		repositoryTypeLabel.setLayoutData(gd_repositoryTypeLabel);
		repositoryTypeLabel.setText("Repository Type :");
		repositoryTypeLabel.setFont(fontBold);
		
		repositoryType = new Label(this, SWT.NONE);
		GridData gd_repositoryType = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_repositoryType.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		repositoryType.setLayoutData(gd_repositoryType);
		repositoryType.setFont(fontNormal);
		
		Label localFolderLabel = new Label(this, SWT.NONE);
		localFolderLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		localFolderLabel.setText("Local Folder :");
		localFolderLabel.setFont(fontBold);
		
		localFolder = new Label(this, SWT.NONE);
		localFolder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		localFolder.setFont(fontNormal);
		
		Label encryptionTypeLabel = new Label(this, SWT.NONE);
		encryptionTypeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		encryptionTypeLabel.setText("Encryption Settings :");
		encryptionTypeLabel.setFont(fontBold);
		
		encryptionType = new Label(this, SWT.NONE);
		encryptionType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		encryptionType.setFont(fontNormal);
	}
	
	public void updateData(){
		final UserInput userInput = getParentWizardDialog().getUserInput();
		
		getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				String type = String.format("%s", userInput.get(SyncanyCommandParameters.COMMAND_ACTION));
				String encryption = String.format("[%s / %s]", userInput.get(SyncanyCommandParameters.ENCRYPTION_ALGORITHM), userInput.get(SyncanyCommandParameters.ENCRYPTION_KEYLENGTH));
				String folder = String.format("%s", userInput.get(SyncanyCommandParameters.LOCAL_FOLDER));
				repositoryType.setText(type);
				localFolder.setText(folder);
				encryptionType.setText(encryption);
			}
		});

	}
	
	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public UserInput getUserSelection() {
		return new UserInput();
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
