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
package org.syncany.gui.settings;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.syncany.gui.ApplicationResourcesManager;
import org.syncany.gui.SWTResourceManager;
import org.syncany.gui.config.ApplicationConfiguration;
import org.syncany.util.CollectionUtil;
import org.syncany.util.I18n;


/**
 * @author vincent
 *
 */
public class ProxySettingsPanel extends Composite {
	private Label proxyTypeLabel;
	private Button radioNoProxy;
	private Button radioAutomaticProxy;
	private Button radioManualProxy;
	private Label proxyAuthTypeLabel;
	private Combo proxyAuthTypeCombo;
	private Label proxyHostPortLabel;
	private Composite composite_2;
	private Text proxyHostText;
	private Label hostPortColonLabel;
	private Text proxyPortText;
	private Button proxyAuthRadio;
	private Label proxyUsernameLabel;
	private Label proxyPasswordLabel;
	private Text proxyUsernameText;
	private Text proxyPasswordText;
	
	private String[] proxyAuthValues = new String[] {"HTTP", "Socket 4", "Socket 5"};
	
	/**
	 * @param parent
	 * @param style
	 */
	public ProxySettingsPanel(Composite parent, int style) {
		super(parent, style);
		initComposite();
	}
	
	public void setApplicationParameters(ApplicationConfiguration configuration){
		proxyUsernameText.setText(configuration.getProxyUsername());
		proxyPasswordText.setText(configuration.getProxyPassword());
		
		//Set proxy auth type
		int idx = CollectionUtil.indexOfElement(proxyAuthValues, configuration.getProxyAuthType());
		if (idx != -1){
			proxyAuthTypeCombo.select(idx);
		}
		
		String authType = configuration.getProxyType().toLowerCase();
		
		switch (authType){
			case "none":
				radioNoProxy.setSelection(true);
				break;
			case "default":
				radioAutomaticProxy.setSelection(true);
				break;
			case "proxy":
				radioManualProxy.setSelection(true);
				break;
		}
		
		proxyHostText.setText(configuration.getProxyHost());
		proxyPortText.setText(configuration.getProxyPort());
		
		proxyAuthRadio.setSelection(configuration.isProxyAuth());
		
		authToggle();
	}

	private void initComposite() {
		Font fontNormal = ApplicationResourcesManager.FONT_NORMAL;
		Font fontBold = ApplicationResourcesManager.FONT_BOLD;
		
		setLayout(new GridLayout(2, false));
		
		proxyTypeLabel = new Label(this, SWT.NONE);
		proxyTypeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		proxyTypeLabel.setText(I18n.getString("dialog.proxy.type", true));
		
		radioNoProxy = new Button(this, SWT.RADIO);
		radioNoProxy.setText(I18n.getString("dialog.proxy.type.no"));
		radioNoProxy.setFont(fontNormal);
		new Label(this, SWT.NONE);
		
		radioAutomaticProxy = new Button(this, SWT.RADIO);
		radioAutomaticProxy.setText(I18n.getString("dialog.proxy.type.default"));
		radioAutomaticProxy.setFont(fontNormal);
		new Label(this, SWT.NONE);
		
		radioManualProxy = new Button(this, SWT.RADIO);
		radioManualProxy.setText(I18n.getString("dialog.proxy.type.manual"));
		radioManualProxy.setFont(fontNormal);
		
		proxyAuthTypeLabel = new Label(this, SWT.NONE);
		proxyAuthTypeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		proxyAuthTypeLabel.setText(I18n.getString("dialog.proxy.auth.type", true));
		proxyAuthTypeLabel.setFont(fontNormal);
		
		proxyAuthTypeCombo = new Combo(this, SWT.NONE);
		proxyAuthTypeCombo.setFont(fontNormal);
		proxyAuthTypeCombo.setItems(proxyAuthValues);
		GridData gd_proxyAuthTypeCombo = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_proxyAuthTypeCombo.heightHint = 49;
		proxyAuthTypeCombo.setLayoutData(gd_proxyAuthTypeCombo);
		
		proxyHostPortLabel = new Label(this, SWT.NONE);
		proxyHostPortLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		proxyHostPortLabel.setFont(fontNormal);
		proxyHostPortLabel.setText(I18n.getString("dialog.proxy.server", true));
		
		composite_2 = new Composite(this, SWT.NONE);
		GridLayout gl_composite_2 = new GridLayout(3, false);
		gl_composite_2.verticalSpacing = 0;
		gl_composite_2.marginWidth = 0;
		gl_composite_2.marginHeight = 0;
		composite_2.setLayout(gl_composite_2);
		composite_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		proxyHostText = new Text(composite_2, SWT.BORDER);
		GridData gd_proxyHostText = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_proxyHostText.heightHint = 15;
		proxyHostText.setLayoutData(gd_proxyHostText);
		proxyHostText.setFont(fontNormal);

		hostPortColonLabel = new Label(composite_2, SWT.NONE);
		hostPortColonLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		hostPortColonLabel.setFont(fontNormal);
		hostPortColonLabel.setText(":");
		
		proxyPortText = new Text(composite_2, SWT.BORDER);
		GridData gd_proxyPortText = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_proxyPortText.widthHint = 50;
		gd_proxyPortText.heightHint = 15;
		proxyPortText.setLayoutData(gd_proxyPortText);
		proxyPortText.setFont(fontNormal);
		new Label(this, SWT.NONE);
		
		proxyAuthRadio = new Button(this, SWT.CHECK);
		proxyAuthRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				authToggle();
			}
		});
		GridData gd_proxyAuthRadio = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_proxyAuthRadio.verticalIndent = 20;
		proxyAuthRadio.setLayoutData(gd_proxyAuthRadio);
		proxyAuthRadio.setFont(fontNormal);
		proxyAuthRadio.setText("Check Button");
		
		proxyUsernameLabel = new Label(this, SWT.NONE);
		proxyUsernameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		proxyUsernameLabel.setFont(fontNormal);
		proxyUsernameLabel.setText(I18n.getString("dialog.proxy.username", true));
		
		proxyUsernameText = new Text(this, SWT.BORDER);
		proxyUsernameText.setFont(fontNormal);
		GridData gd_proxyUsernameText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_proxyUsernameText.heightHint = 15;
		proxyUsernameText.setLayoutData(gd_proxyUsernameText);
		
		proxyPasswordLabel = new Label(this, SWT.NONE);
		proxyPasswordLabel.setFont(fontNormal);
		proxyPasswordLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		proxyPasswordLabel.setText(I18n.getString("dialog.proxy.password", true));
		
		proxyPasswordText = new Text(this, SWT.BORDER |SWT.PASSWORD);
		proxyPasswordText.setFont(fontNormal);
		GridData gd_proxyPasswordText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_proxyPasswordText.heightHint = 15;
		proxyPasswordText.setLayoutData(gd_proxyPasswordText);	
	}

	protected void authToggle() {
		proxyPasswordText.setEnabled(proxyAuthRadio.getSelection());
		proxyUsernameText.setEnabled(proxyAuthRadio.getSelection());
		
		Color black = SWTResourceManager.getColor(SWT.COLOR_BLACK);
		Color gray = SWTResourceManager.getColor(SWT.COLOR_GRAY);
		
		proxyUsernameLabel.setForeground(proxyAuthRadio.getSelection() ? black : gray);
		proxyPasswordLabel.setForeground(proxyAuthRadio.getSelection() ? black : gray);
	}

	public Map<String, String> getInputParameters() {
		Map<String, String> params = new HashMap<>();
		params.put("proxy.auth", proxyAuthRadio.getSelection()+"");
		params.put("proxy.host", proxyHostText.getText());
		params.put("proxy.username", proxyUsernameText.getText());
		params.put("proxy.password", proxyPasswordText.getText());
		params.put("proxy.type", proxyAuthTypeCombo.getItem(proxyAuthTypeCombo.getSelectionIndex()).toString());
		params.put("proxy.port", proxyPortText.getText());
		return params;
	}
}
