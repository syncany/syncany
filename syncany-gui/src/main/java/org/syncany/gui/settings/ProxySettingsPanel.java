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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.syncany.gui.SWTResourceManager;
import org.syncany.gui.WidgetDecorator;
import org.syncany.gui.config.ApplicationConfiguration;
import org.syncany.gui.config.ProxyController;
import org.syncany.gui.util.I18n;


/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
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
	private Label lblNewLabel;
	private Label proxyIntroductionTitleLabel;
	private Label proxyIntroductionLabel;
	
	/**
	 * @param parent
	 * @param style
	 */
	public ProxySettingsPanel(Composite parent, int style) {
		super(parent, style);
		initComposite();
	}
	
	private void initComposite() {
		setLayout(new GridLayout(2, false));
		
		proxyIntroductionTitleLabel = new Label(this, SWT.NONE);
		proxyIntroductionTitleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		proxyIntroductionTitleLabel.setText(I18n.getString("dialog.settings.proxy.introduction.title"));
		
		proxyIntroductionLabel = new Label(this, SWT.NONE);
		proxyIntroductionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		proxyIntroductionLabel.setText(I18n.getString("dialog.settings.proxy.introduction"));
		
		lblNewLabel = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
		lblNewLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		
		proxyTypeLabel = new Label(this, SWT.NONE);
		proxyTypeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 3));
		proxyTypeLabel.setText(I18n.getString("dialog.settings.proxy.type", true));
		
		radioNoProxy = new Button(this, SWT.RADIO);
		radioNoProxy.setText(I18n.getString("dialog.settings.proxy.type.no"));
		radioNoProxy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				proxyToggle();
			}
		});
		
		radioAutomaticProxy = new Button(this, SWT.RADIO);
		radioAutomaticProxy.setText(I18n.getString("dialog.settings.proxy.type.default"));
		radioAutomaticProxy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				proxyToggle();
			}
		});
		
		radioManualProxy = new Button(this, SWT.RADIO);
		radioManualProxy.setText(I18n.getString("dialog.settings.proxy.type.manual"));
		radioManualProxy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				proxyToggle();
			}
		});
		
		proxyAuthTypeLabel = new Label(this, SWT.NONE);
		proxyAuthTypeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		proxyAuthTypeLabel.setText(I18n.getString("dialog.settings.proxy.auth.type", true));
		
		proxyAuthTypeCombo = new Combo(this, SWT.NONE);
		proxyAuthTypeCombo.setItems(proxyAuthValues);
		proxyAuthTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		proxyHostPortLabel = new Label(this, SWT.NONE);
		proxyHostPortLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		proxyHostPortLabel.setText(I18n.getString("dialog.settings.proxy.server", true));
		
		composite_2 = new Composite(this, SWT.NONE);
		GridLayout gl_composite_2 = new GridLayout(3, false);
		gl_composite_2.verticalSpacing = 0;
		gl_composite_2.marginWidth = 0;
		gl_composite_2.marginHeight = 0;
		composite_2.setLayout(gl_composite_2);
		composite_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		proxyHostText = new Text(composite_2, SWT.BORDER);
		GridData gd_proxyHostText = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_proxyHostText.heightHint = 20;
		proxyHostText.setLayoutData(gd_proxyHostText);

		hostPortColonLabel = new Label(composite_2, SWT.NONE);
		hostPortColonLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		hostPortColonLabel.setText(":");
		
		proxyPortText = new Text(composite_2, SWT.BORDER);
		GridData gd_proxyPortText = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_proxyPortText.widthHint = 50;
		gd_proxyPortText.heightHint = 20;
		proxyPortText.setLayoutData(gd_proxyPortText);
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
		proxyAuthRadio.setText(I18n.getString("dialog.settings.proxy.auth"));
		
		proxyUsernameLabel = new Label(this, SWT.NONE);
		proxyUsernameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		proxyUsernameLabel.setText(I18n.getString("dialog.settings.proxy.username", true));
		
		proxyUsernameText = new Text(this, SWT.BORDER);
		GridData gd_proxyUsernameText = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_proxyUsernameText.heightHint = 20;
		proxyUsernameText.setLayoutData(gd_proxyUsernameText);
		
		proxyPasswordLabel = new Label(this, SWT.NONE);
		proxyPasswordLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		proxyPasswordLabel.setText(I18n.getString("dialog.settings.proxy.password", true));
		
		proxyPasswordText = new Text(this, SWT.BORDER |SWT.PASSWORD);
		GridData gd_proxyPasswordText = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_proxyPasswordText.heightHint = 20;
		proxyPasswordText.setLayoutData(gd_proxyPasswordText);
		
		WidgetDecorator.bold(proxyIntroductionTitleLabel);
		WidgetDecorator.normal(
			proxyIntroductionLabel, 
			proxyTypeLabel,
			radioNoProxy, radioAutomaticProxy, radioManualProxy, 
			proxyAuthTypeLabel, proxyAuthTypeCombo, proxyHostPortLabel,
			proxyHostText, hostPortColonLabel,
			proxyPortText, proxyAuthRadio,
			proxyUsernameLabel, proxyPasswordLabel,
			proxyUsernameText, proxyPasswordText
		);
	}
	
	/**
	 * enables / disables username/password fields
	 * depending on proxyAuthRadio selection
	 */
	private void authToggle() {
		Color black = SWTResourceManager.getColor(SWT.COLOR_BLACK);
		Color gray = SWTResourceManager.getColor(SWT.COLOR_GRAY);

		proxyPasswordText.setEnabled(proxyAuthRadio.getSelection() && radioManualProxy.getSelection());
		proxyUsernameText.setEnabled(proxyAuthRadio.getSelection() && radioManualProxy.getSelection());
		
		proxyUsernameLabel.setForeground(proxyAuthRadio.getSelection() && radioManualProxy.getSelection() ? black : gray);
		proxyPasswordLabel.setForeground(proxyAuthRadio.getSelection() && radioManualProxy.getSelection() ? black : gray);
	}
	
	private void proxyToggle(){
		Color black = SWTResourceManager.getColor(SWT.COLOR_BLACK);
		Color gray = SWTResourceManager.getColor(SWT.COLOR_GRAY);
		
		proxyAuthTypeLabel.setForeground(radioManualProxy.getSelection() ? black : gray);
		proxyHostPortLabel.setForeground(radioManualProxy.getSelection() ? black : gray);
		hostPortColonLabel.setForeground(radioManualProxy.getSelection() ? black : gray);
		
		proxyPortText.setEnabled(radioManualProxy.getSelection());
		proxyHostText.setEnabled(radioManualProxy.getSelection());
		proxyAuthTypeCombo.setEnabled(radioManualProxy.getSelection());
		proxyAuthRadio.setEnabled(radioManualProxy.getSelection());
		
		authToggle();
	}

	public Map<String, String> getInputParameters() {
		Map<String, String> params = new HashMap<>();
		params.put("proxy.auth", proxyAuthRadio.getSelection()+"");
		params.put("proxy.host", proxyHostText.getText());
		params.put("proxy.username", proxyUsernameText.getText());
		params.put("proxy.password", proxyPasswordText.getText());
		params.put("proxy.type", 
			radioNoProxy.getSelection() ? ProxyController.ProxyType.NONE.toString() :
			radioAutomaticProxy.getSelection() ? ProxyController.ProxyType.DEFAULT.toString() :
			ProxyController.ProxyType.PROXY.toString()
		);
		
		int idx = proxyAuthTypeCombo.getSelectionIndex();
		
		if (idx != -1){
			params.put("proxy.authType", proxyAuthTypeCombo.getItem(idx).toString());
		}
		
		params.put("proxy.port", proxyPortText.getText());
		return params;
	}
	
	public void setApplicationParameters(ApplicationConfiguration configuration){
		if (configuration.getProxyUsername() != null)
			proxyUsernameText.setText(configuration.getProxyUsername());
		
		if (configuration.getProxyPassword() != null)
			proxyPasswordText.setText(configuration.getProxyPassword());
		
		//Set proxy auth type
		int idx = 1;//CollectionUtil.indexOfElement(proxyAuthValues, configuration.getProxyAuthType());
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
		
		if (configuration.getProxyHost() != null)
			proxyHostText.setText(configuration.getProxyHost());
		
		if (configuration.getProxyPort() != null)
			proxyPortText.setText(configuration.getProxyPort());
		
		proxyAuthRadio.setSelection(configuration.isProxyAuth());
		
		proxyToggle();
	}
}
