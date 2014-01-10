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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.syncany.gui.ApplicationResourcesManager;
import org.syncany.gui.SWTResourceManager;
import org.syncany.gui.SWTUtil;
import org.syncany.gui.SyncanyCommandParameters;
import org.syncany.gui.UserInput;
import org.syncany.gui.WidgetDecorator;
import org.syncany.gui.WidgetDecorator.FontDecorator;
import org.syncany.util.I18n;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class RepositoryEncryptionPanel extends WizardPanelComposite {
	private Text password;
	private Text passwordAgain;
	private Combo cypherCombo;
	private Combo keylengthCombo;
	private Spinner chunckSize;
	private Button enableEncryption;
	private Label lblNewLabel_6; 
	private Label lblNewLabel_5;
	private Label lblNewLabel_4;
	
	public RepositoryEncryptionPanel(WizardDialog wizardParentDialog, Composite parent, int style) {
		super(wizardParentDialog, parent, style);
		initComposite();
		enableEncryption.setSelection(false);
		toggleEncryptionSelection();
	}
	
	public void initComposite(){
		GridLayout gridLayout = new GridLayout(2, false);
		setLayout(gridLayout);
		
		Label introductionTextTitle = new Label(this, SWT.WRAP);
		introductionTextTitle.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
		introductionTextTitle.setText(I18n.getString("repository.encryption.introduction.title"));
		WidgetDecorator.decorateLabel(introductionTextTitle, FontDecorator.BOLD);
		
		Label introductionText = new Label(this, SWT.WRAP);
		introductionText.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
		introductionText.setText(I18n.getString("repository.encryption.introduction"));
		WidgetDecorator.decorateLabel(introductionText, FontDecorator.NORMAL);
		
		Label passwordLabel = new Label(this, SWT.NONE);
		passwordLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		passwordLabel.setText(I18n.getString("repository.encryption.password", true));
		WidgetDecorator.decorateLabel(passwordLabel, FontDecorator.NORMAL);
		
		password = new Text(this, SWT.BORDER | SWT.PASSWORD);
		WidgetDecorator.decorateText(password, FontDecorator.NORMAL);
		password.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label passwordLabelAgain = new Label(this, SWT.NONE);
		passwordLabelAgain.setText(I18n.getString("repository.encryption.passwordAgain", true));
		WidgetDecorator.decorateLabel(passwordLabelAgain, FontDecorator.NORMAL);
		
		passwordAgain = new Text(this, SWT.BORDER | SWT.PASSWORD);
		WidgetDecorator.decorateText(passwordAgain, FontDecorator.NORMAL);
		passwordAgain.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		enableEncryption = new Button(this, SWT.CHECK);
		WidgetDecorator.setFont(enableEncryption, FontDecorator.NORMAL);
		GridData gd_enableEncryption = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		gd_enableEncryption.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		enableEncryption.setLayoutData(gd_enableEncryption);
		enableEncryption.setText(I18n.getString("repository.encryption.enable"));
		enableEncryption.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				toggleEncryptionSelection();
			}
		});
		
		Composite composite = new Composite(this, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		GridData gd_composite = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gd_composite.widthHint = 411;
		composite.setLayoutData(gd_composite);
		
		lblNewLabel_4 = new Label(composite, SWT.NONE);
		GridData gd_lblNewLabel_4 = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_lblNewLabel_4.horizontalIndent = 30;
		lblNewLabel_4.setLayoutData(gd_lblNewLabel_4);
		lblNewLabel_4.setText(I18n.getString("repository.encryption.algorithm", true));
		WidgetDecorator.decorateLabel(lblNewLabel_4, FontDecorator.NORMAL);
		
		cypherCombo = new Combo(composite, SWT.NONE);
		cypherCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		WidgetDecorator.setFont(cypherCombo, FontDecorator.NORMAL);
		cypherCombo.setItems(new String[]{"AES", "TwoFish"});
		cypherCombo.select(0);
		
		lblNewLabel_5 = new Label(composite, SWT.NONE);
		GridData gd_lblNewLabel_5 = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_lblNewLabel_5.horizontalIndent = 30;
		lblNewLabel_5.setLayoutData(gd_lblNewLabel_5);
		lblNewLabel_5.setText(I18n.getString("repository.encryption.keylength", true));
		WidgetDecorator.decorateLabel(lblNewLabel_5, FontDecorator.NORMAL);
		
		keylengthCombo = new Combo(composite, SWT.NONE);
		keylengthCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		WidgetDecorator.setFont(keylengthCombo, FontDecorator.NORMAL);
		keylengthCombo.setItems(new String[]{"128", "256"});
		keylengthCombo.select(0);
		
		lblNewLabel_6 = new Label(composite, SWT.NONE);
		GridData gd_lblNewLabel_6 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblNewLabel_6.horizontalIndent = 30;
		lblNewLabel_6.setLayoutData(gd_lblNewLabel_6);
		lblNewLabel_6.setText(I18n.getString("repository.encryption.chunckSize", true));
		WidgetDecorator.decorateLabel(lblNewLabel_6, FontDecorator.NORMAL);
		
		chunckSize = new Spinner(composite, SWT.BORDER);
		chunckSize.setMaximum(10000);
		chunckSize.setSelection(512);
		GridData gd_chunckSize = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_chunckSize.heightHint = 15;
		chunckSize.setLayoutData(gd_chunckSize);
		WidgetDecorator.setFont(keylengthCombo, FontDecorator.NORMAL);
	}
	
	protected void toggleEncryptionSelection() {
		chunckSize.setEnabled(enableEncryption.getSelection());
		keylengthCombo.setEnabled(enableEncryption.getSelection());
		cypherCombo.setEnabled(enableEncryption.getSelection());
		
		Color black = SWTResourceManager.getColor(SWT.COLOR_BLACK);
		Color gray = SWTResourceManager.getColor(SWT.COLOR_GRAY);
		
		lblNewLabel_4.setForeground(enableEncryption.getSelection() ? black : gray);
		lblNewLabel_5.setForeground(enableEncryption.getSelection() ? black : gray);
		lblNewLabel_6.setForeground(enableEncryption.getSelection() ? black : gray);
	}

	public boolean issValid() {
		return 
			password.getText().length() > 6 && 
			password.getText().equals(passwordAgain.getText()); 
	}
	
	@Override
	public boolean isValid() {
		boolean valid = true;
		
		// && order matters cause java uses lazy evaluation
		valid = SWTUtil.checkTextLength(password, 6) && valid;
		valid = SWTUtil.checkEquals(passwordAgain, password.getText()) && valid;
			
		return valid;
	}

	@Override
	public UserInput getUserSelection() {
		UserInput userInput = new UserInput();
		userInput.put(SyncanyCommandParameters.ENCRYPTION_PASSWORD, password.getText());
		
		if (enableEncryption.getSelection()){
			userInput.put(SyncanyCommandParameters.ENCRYPTION_ENABLED, enableEncryption.getSelection() ? "yes" : "no");
			userInput.put(SyncanyCommandParameters.ENCRYPTION_ALGORITHM, cypherCombo.getItem(cypherCombo.getSelectionIndex()));
			userInput.put(SyncanyCommandParameters.ENCRYPTION_KEYLENGTH, keylengthCombo.getItem(keylengthCombo.getSelectionIndex()));
		}
		return userInput;
	}

	@Override
	public boolean hasNextButton() {
		return true;
	}

	@Override
	public boolean hasPreviousButton() {
		return true;
	}

	@Override
	public boolean hasFinishButton() {
		return false;
	}

	@Override
	public void updateData() {
		
	}
}
