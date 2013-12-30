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
package org.syncany.gui.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.syncany.gui.panel.PluginPanel;
import org.syncany.util.I18n;

/**
 * @author vincent
 *
 */
public class RestPluginPanel extends PluginPanel {
	private static final Logger log = Logger.getLogger(RestPluginPanel.class.getSimpleName());
	private static final int TIMEOUT_CONNECT = 5000;
	
	private Text hostText;
	private Text usernameText;
	private Text passwordText;
	private Text pathText;
	private Text portText;

	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public RestPluginPanel(Composite parent, int style) {
		super(parent, style);
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(2, false);
		gl_composite.verticalSpacing = 15;
		setLayout(gl_composite);
		
		Label hostLabel = new Label(this, SWT.NONE);
		hostLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		hostLabel.setText(I18n.getString("NewPluginDialog.dialog.ftpcomposite.host"));
		
		hostText = new Text(this, SWT.BORDER);
		hostText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label usernameLabel = new Label(this, SWT.NONE);
		usernameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		usernameLabel.setText(I18n.getString("NewPluginDialog.dialog.ftpcomposite.username"));
		
		passwordText = new Text(this, SWT.BORDER);
		passwordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label passwordLabel = new Label(this, SWT.NONE);
		passwordLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		passwordLabel.setText(I18n.getString("NewPluginDialog.dialog.ftpcomposite.password"));
		
		usernameText = new Text(this, SWT.BORDER);
		usernameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label pathLabel = new Label(this, SWT.NONE);
		pathLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		pathLabel.setText(I18n.getString("NewPluginDialog.dialog.ftpcomposite.path"));
		
		pathText = new Text(this, SWT.BORDER);
		pathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label portLabel = new Label(this, SWT.NONE);
		portLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		portLabel.setText(I18n.getString("NewPluginDialog.dialog.ftpcomposite.port"));
		
		portText = new Text(this, SWT.BORDER);
		portText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Composite buttonComposite = new Composite(this, SWT.NONE);
		GridLayout gl_buttonComposite = new GridLayout(2, false);
		gl_buttonComposite.horizontalSpacing = 0;
		gl_buttonComposite.verticalSpacing = 0;
		gl_buttonComposite.marginWidth = 0;
		gl_buttonComposite.marginHeight = 0;
		buttonComposite.setLayout(gl_buttonComposite);
		GridData gd_buttonComposite = new GridData(SWT.FILL, SWT.TOP, true, true, 2, 1);
		gd_buttonComposite.minimumHeight = 30;
		buttonComposite.setLayoutData(gd_buttonComposite);
		
		final Label testResultLabel = new Label(buttonComposite, SWT.NONE);
		testResultLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		testResultLabel.setAlignment(SWT.CENTER);
		
		final Button testFtpButton = new Button(buttonComposite, SWT.NONE);

		GridData gd_testFtpButton = new GridData(SWT.CENTER, SWT.FILL, false, false, 1, 1);
		gd_testFtpButton.heightHint = 30;
		gd_testFtpButton.widthHint = 80;
		testFtpButton.setLayoutData(gd_testFtpButton);
		testFtpButton.setText(I18n.getString("NewPluginDialog.dialog.ftpcomposite.test"));
		testFtpButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			}
		});
	}

	@Override
	protected void checkSubclass() {
		
	}

	@Override
	public Map<String, String> getParameters() {
		Map<String, String> parameters = new HashMap<>();
		parameters.put("host", hostText.getText());
		parameters.put("username", usernameText.getText());
		parameters.put("password", passwordText.getText());
		parameters.put("path", pathText.getText());
		parameters.put("port", portText.getText());
		return parameters;
	}
	
	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return true;
	}
}
