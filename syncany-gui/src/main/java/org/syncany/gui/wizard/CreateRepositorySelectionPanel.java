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
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.gui.panel.ApplicationResources;
import org.syncany.util.I18n;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class CreateRepositorySelectionPanel extends WizardPanelComposite {
	private Button emailOptionRadio;
	private Button ftpOptionRadio;
	private Button otherPluginRadio;
	private Combo pluginSelectionCombo;
	private List<Plugin> pluginList = Plugins.list();
	private Label lblNewLabel;
	private Label lblNewLabel_1;
	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public CreateRepositorySelectionPanel(Composite parent, int style) {
		super(parent, style);
		initComposite();
	}
	
	private void initComposite(){
		Font fontNormal = ApplicationResources.FONT_NORMAL;
		Font fontBold = ApplicationResources.FONT_BOLD;
		
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(2, false);
		setLayout(gl_composite);
		
		lblNewLabel = new Label(this, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		lblNewLabel.setFont(fontBold);
		lblNewLabel.setText("New Label");
		
		lblNewLabel_1 = new Label(this, SWT.NONE);
		lblNewLabel_1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		lblNewLabel_1.setFont(fontNormal);
		lblNewLabel_1.setText("New Label");
		
		emailOptionRadio = new Button(this, SWT.RADIO);
		emailOptionRadio.setFont(fontNormal);
		GridData gd_emailOptionRadio = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_emailOptionRadio.verticalIndent = 20;
		gd_emailOptionRadio.horizontalIndent = 30;
		gd_emailOptionRadio.heightHint = 30;
		gd_emailOptionRadio.minimumHeight = 30;
		emailOptionRadio.setLayoutData(gd_emailOptionRadio);
		emailOptionRadio.setBounds(0, 0, 90, 16);
		emailOptionRadio.setText(I18n.getString("NewDialog.dialog.option.email"));
		emailOptionRadio.setSelection(true);
		
		ftpOptionRadio = new Button(this, SWT.RADIO);
		ftpOptionRadio.setFont(fontNormal);
		GridData gd_ftpOptionRadio = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_ftpOptionRadio.horizontalIndent = 30;
		gd_ftpOptionRadio.heightHint = 30;
		gd_ftpOptionRadio.minimumHeight = 30;
		ftpOptionRadio.setLayoutData(gd_ftpOptionRadio);
		ftpOptionRadio.setBounds(0, 0, 90, 16);
		ftpOptionRadio.setText(I18n.getString("NewDialog.dialog.option.ftp"));
		
		otherPluginRadio = new Button(this, SWT.RADIO);
		otherPluginRadio.setFont(fontNormal);
		GridData gd_otherPluginRadio = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
		gd_otherPluginRadio.horizontalIndent = 30;
		gd_otherPluginRadio.heightHint = 30;
		gd_otherPluginRadio.minimumHeight = 30;
		otherPluginRadio.setLayoutData(gd_otherPluginRadio);
		otherPluginRadio.setText(I18n.getString("NewDialog.dialog.option.otherPlugin"));
		
		pluginSelectionCombo = new Combo(this, SWT.NONE);
		pluginSelectionCombo.setFont(fontNormal);
		for (Plugin p : pluginList){
			pluginSelectionCombo.add(p.getName());
		}
		pluginSelectionCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		pluginSelectionCombo.setEnabled(false);
		pluginSelectionCombo.select(0);
		
		otherPluginRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pluginSelectionCombo.setEnabled(otherPluginRadio.getSelection());
			}
		});
	}

	@Override
	public boolean isValid() {
		return emailOptionRadio.getSelection() || ftpOptionRadio.getSelection() || otherPluginRadio.getSelection();
	}

	@Override
	public Map<String, String> getUserSelection() {
		Map<String, String> userInput = new HashMap<>();
		if (emailOptionRadio.getSelection()){
			userInput.put("repositoryType", "email");
		}
		else if (ftpOptionRadio.getSelection()){
			userInput.put("repositoryType", "ftp");
		}
		else{
			userInput.put("repositoryType", pluginSelectionCombo.getItem(pluginSelectionCombo.getSelectionIndex()).toLowerCase());
		}
		return userInput;
	}
}
