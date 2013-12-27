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
import org.eclipse.swt.widgets.Text;
import org.syncany.gui.util.EmailValidator;
import org.syncany.util.I18n;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class RepositoryEmailPanel extends WizardPanelComposite {
	private Text emailTextField;
	private Text passwordTextField;
	
	public RepositoryEmailPanel(Composite shell, int style) {
		super(shell, style);
		initComposite();
	}
	
	protected void initComposite(){
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(2, false);
		gl_composite.verticalSpacing = 15;
		setLayout(gl_composite);
		
		Label emailLabel = new Label(this, SWT.NONE);
		emailLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		emailLabel.setText(I18n.getString("NewEmailDialog.dialog.email"));
		
		emailTextField = new Text(this, SWT.BORDER);
		emailTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label passwordLabel = new Label(this, SWT.NONE);
		passwordLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		passwordLabel.setText(I18n.getString("NewEmailDialog.dialog.password"));
		
		passwordTextField = new Text(this, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
		passwordTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
	}

	@Override
	public boolean isValid() {
		String email = emailTextField.getText();
		EmailValidator emailValidator = new EmailValidator();
		return emailValidator.validate(email) && passwordTextField.getText().length() > 8;
	}

	@Override
	public Map<String, String> getUserSelection() {
		// TODO Auto-generated method stub
		return null;
	}
}
