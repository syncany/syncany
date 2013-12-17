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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.gui.util.I18n;
import org.syncany.gui.wizard.core.DefaultWizardPanel;
import org.syncany.gui.wizard.core.WizardAction;
import org.syncany.gui.wizard.core.WizardType;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class NewDialog extends DefaultWizardPanel {
	private Button emailOptionRadio;
	private Button ftpOptionRadio;
	private Button otherPluginRadio;
	private Combo pluginSelectionCombo;
	private List<Plugin> pluginList = Plugins.list();
	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public NewDialog(Shell parent, int style) {
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

		emailOptionRadio = new Button(composite, SWT.RADIO);
		emailOptionRadio.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		emailOptionRadio.setBounds(0, 0, 90, 16);
		emailOptionRadio.setText(I18n.getString("NewDialog.dialog.option.email"));
		emailOptionRadio.setSelection(true);
		
		ftpOptionRadio = new Button(composite, SWT.RADIO);
		ftpOptionRadio.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		ftpOptionRadio.setBounds(0, 0, 90, 16);
		ftpOptionRadio.setText(I18n.getString("NewDialog.dialog.option.ftp"));
		
		otherPluginRadio = new Button(composite, SWT.RADIO);
		otherPluginRadio.setText(I18n.getString("NewDialog.dialog.option.otherPlugin"));
		
		pluginSelectionCombo = new Combo(composite, SWT.NONE);
		for (Plugin p : pluginList){
			pluginSelectionCombo.add(p.getName());
		}
		pluginSelectionCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		pluginSelectionCombo.setEnabled(false);
		
		otherPluginRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pluginSelectionCombo.setEnabled(otherPluginRadio.getSelection());
			}
		});
		return composite;
	}

	@Override
	protected void handleAction(WizardAction action) {
		if (action == WizardAction.NEXT){
			if (emailOptionRadio.getSelection()){
				this.shell.dispose();
				NewEmailDialog sd = new NewEmailDialog(getParent(), SWT.APPLICATION_MODAL);
				sd.open();
			}
			else if (ftpOptionRadio.getSelection()){
				this.shell.dispose();
				String className = "org.syncany.gui.panel.plugin.FTPComposite";
				NewPluginDialog sd = new NewPluginDialog(className, getParent(), SWT.APPLICATION_MODAL);
				sd.open();
			}
			else if (otherPluginRadio.getSelection()){
				String selectedPlugin = pluginSelectionCombo.getItem(pluginSelectionCombo.getSelectionIndex());
				String className = null;
				switch (selectedPlugin.toLowerCase()){
					case "rest":
						className = "org.syncany.gui.panel.plugin.RestComposite";
						break;
					case "amazon s3":
						className = "org.syncany.gui.panel.plugin.AmazonComposite";
						break;
				}

				this.shell.dispose();
				NewPluginDialog sd = new NewPluginDialog(className, getParent(), SWT.APPLICATION_MODAL);
				sd.open();
			}
		}
		else if (action == WizardAction.PREVIOUS){
			this.shell.dispose();
			StartDialog sd = new StartDialog(getParent(), SWT.APPLICATION_MODAL);
			sd.open();
		}
	}
}
