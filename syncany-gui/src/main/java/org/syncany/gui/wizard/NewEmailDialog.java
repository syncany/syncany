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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.syncany.gui.util.I18n;
import org.syncany.gui.wizard.core.DefaultWizardPanel;
import org.syncany.gui.wizard.core.WizardAction;
import org.syncany.gui.wizard.core.WizardType;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class NewEmailDialog extends DefaultWizardPanel {
	private Text emailTextField;
	private Text passwordTextField;
	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public NewEmailDialog(Shell parent, int style) {
		super(WizardType.NEXT | WizardType.PREVIOUS, parent, style);
	}
	
	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {
		super.createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return null;
	}
	
	protected Composite createComposite(Shell shell){
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(2, false);
		gl_composite.verticalSpacing = 15;
		composite.setLayout(gl_composite);
		
		Label emailLabel = new Label(composite, SWT.NONE);
		emailLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		emailLabel.setText(I18n.getString("NewEmailDialog.dialog.email"));
		
		emailTextField = new Text(composite, SWT.BORDER);
		emailTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label passwordLabel = new Label(composite, SWT.NONE);
		passwordLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		passwordLabel.setText(I18n.getString("NewEmailDialog.dialog.password"));
		
		passwordTextField = new Text(composite, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
		passwordTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		return composite;
	}

	@Override
	protected void handleAction(WizardAction action) {
		if (action == WizardAction.NEXT){
			this.shell.dispose();
			NewLocalFolders sd = new NewLocalFolders(getParent(), SWT.APPLICATION_MODAL);
			sd.open();
		}
		else if (action == WizardAction.PREVIOUS){
			this.shell.dispose();
			NewDialog sd = new NewDialog(getParent(), SWT.APPLICATION_MODAL);
			sd.open();
		}
	}
}
