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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.syncany.gui.wizard.core.DefaultWizardPanel;
import org.syncany.gui.wizard.core.WizardAction;
import org.syncany.gui.wizard.core.WizardType;
import org.syncany.util.I18n;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class StartDialog extends DefaultWizardPanel {
	protected Object result;

	private Button createStorageRadio;
	private Button connectStorageRadio;
	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public StartDialog(Map<String, Object> params, Shell parent, int style) {
		super(params, WizardType.NEXT, parent, style);
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
		
		return result;
	}
	
	protected Composite createComposite(Shell shell){
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(1, false);
		gl_composite.verticalSpacing = 15;
		composite.setLayout(gl_composite);

		createStorageRadio = new Button(composite, SWT.RADIO);
		GridData gd_createStorageRadio = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_createStorageRadio.heightHint = 30;
		createStorageRadio.setLayoutData(gd_createStorageRadio);
		createStorageRadio.setBounds(0, 0, 90, 16);
		createStorageRadio.setText(I18n.getString("StartDialog.dialog.option.createOnlineStorage"));
		createStorageRadio.setSelection(true);
		
		connectStorageRadio = new Button(composite, SWT.RADIO);
		GridData gd_connectStorageRadio = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_connectStorageRadio.heightHint = 30;
		connectStorageRadio.setLayoutData(gd_connectStorageRadio);
		connectStorageRadio.setBounds(0, 0, 90, 16);
		connectStorageRadio.setText(I18n.getString("StartDialog.dialog.option.connectExisting"));
		
		return composite;
	}

	@Override
	protected void handleAction(WizardAction action) {
		if (action == WizardAction.NEXT){
			if (createStorageRadio.getSelection()){
				this.shell.dispose();
				NewDialog sd = new NewDialog(getWizardParameters(), getParent(), SWT.APPLICATION_MODAL);
				sd.open();
			}
			else if (connectStorageRadio.getSelection()){
				this.shell.dispose();
				ConnectDialog sd = new ConnectDialog(getWizardParameters(), getParent(), SWT.APPLICATION_MODAL);
				sd.open();
			}
		}
	}
}
