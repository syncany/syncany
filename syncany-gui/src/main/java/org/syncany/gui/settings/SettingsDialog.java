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

import static org.syncany.gui.ApplicationResourcesManager.DEFAULT_BUTTON_HEIGHT;
import static org.syncany.gui.ApplicationResourcesManager.DEFAULT_BUTTON_WIDTH;

import java.io.File;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.syncany.gui.ApplicationResourcesManager;
import org.syncany.gui.Launcher;
import org.syncany.gui.config.ApplicationConfiguration;
import org.syncany.gui.config.ApplicationConfigurationTO;
import org.syncany.gui.util.DialogUtil;
import org.syncany.util.I18n;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class SettingsDialog extends Dialog {
	protected Object result;
	protected Shell shell;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public SettingsDialog(Shell parent, int style) {
		super(parent, style);
		setText(I18n.getString("dialog.settings.title"));
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {
		createContents();
		
		updateContent();
		
		DialogUtil.centerOnScreen(shell);
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
	
	private void updateContent() {
		proxySettingsPanel.setApplicationParameters(Launcher.applicationConfiguration);
	}

	public static void main(String[] args) {
		new SettingsDialog(new Shell(), SWT.APPLICATION_MODAL).open();
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		Font fontNormal = ApplicationResourcesManager.FONT_NORMAL;
		Font fontBold = ApplicationResourcesManager.FONT_BOLD;
		
		shell = new Shell(getParent(), SWT.DIALOG_TRIM);
		shell.setSize(516, 360);
		shell.setText(getText());
		GridLayout gl_shell = new GridLayout(3, false);
		gl_shell.marginHeight = 0;
		gl_shell.marginWidth = 0;
		gl_shell.horizontalSpacing = 0;
		gl_shell.verticalSpacing = 0;
		shell.setLayout(gl_shell);
		
		tree = new Tree(shell, SWT.NONE);
		tree.setFont(fontNormal);
		tree.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleTreeSelection();
			}
		});
		GridData gd_tree = new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1);
		gd_tree.widthHint = 100;
		tree.setLayoutData(gd_tree);
		
		menuItemOptions = new TreeItem(tree, SWT.NONE);
		menuItemOptions.setText("Options");
		
		menuItemGeneral = new TreeItem(menuItemOptions, SWT.NONE);
		menuItemGeneral.setText("General");
		menuItemGeneral.setFont(fontNormal);
		
		menuItemProxy = new TreeItem(menuItemOptions, SWT.NONE);
		menuItemProxy.setText("Proxy");
		menuItemProxy.setExpanded(true);
		menuItemProxy.setFont(fontNormal);
		
		menuItemAccount = new TreeItem(menuItemOptions, SWT.NONE);
		menuItemAccount.setText("Account");
		menuItemAccount.setFont(fontNormal);
		menuItemOptions.setExpanded(true);
		
		lblNewLabel_1 = new Label(shell, SWT.SEPARATOR);
		lblNewLabel_1.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true, 1, 1));
		lblNewLabel_1.setText("New Label");
		lblNewLabel_1.setFont(fontNormal);
		
		composite = new Composite(shell, SWT.NONE);
		stackLayout = new StackLayout();
		composite.setLayout(stackLayout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		defaultPanel = new Composite(composite, SWT.NONE);
		defaultPanel.setLayout(new GridLayout(1, false));
		
		optionsMessageTitleLabel = new Label(defaultPanel, SWT.WRAP);
		optionsMessageTitleLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		optionsMessageTitleLabel.setText(I18n.getString("dialog.settings.default.message.title"));
		optionsMessageTitleLabel.setFont(fontBold);
		
		optionsMessageLabel = new Label(defaultPanel, SWT.WRAP);
		optionsMessageLabel.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true, 1, 1));
		optionsMessageLabel.setText(I18n.getString("dialog.settings.default.message.title"));
		optionsMessageLabel.setFont(fontNormal);
		
		generalSettingsPanel = new GeneralSettingsPanel(composite, SWT.NONE);
		proxySettingsPanel = new ProxySettingsPanel(composite, SWT.NONE);
		accountSettingsPanel = new AccountSettingsPanel(composite, SWT.NONE);
		
		horizontalSeparatorLabel = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		horizontalSeparatorLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
		
		Composite buttonComposite = new Composite(shell, SWT.NONE);
		buttonComposite.setLayout(new RowLayout(SWT.HORIZONTAL));
		buttonComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 3, 1));
		
		Button cancelButton = new Button(buttonComposite, SWT.NONE);
		cancelButton.setLayoutData(new RowData(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
		cancelButton.setText(I18n.getString("dialog.default.cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleCancel();
			}
		});
		
		Button okButton = new Button(buttonComposite, SWT.NONE);
		okButton.setLayoutData(new RowData(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
		okButton.setText(I18n.getString("dialog.default.ok"));
		okButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleOk();
			}
		});
	}
	
	private void handleCancel(){
		shell.dispose();
	}
	
	private void handleOk(){
		//Save settings
		Map<String, String> proxyParams = proxySettingsPanel.getInputParameters();
		
		Launcher.applicationConfiguration.setProxyAuth(Boolean.parseBoolean(proxyParams.get("proxy.auth")));
		Launcher.applicationConfiguration.setProxyHost(proxyParams.get("proxy.host"));
		Launcher.applicationConfiguration.setProxyPassword(proxyParams.get("proxy.password"));
		Launcher.applicationConfiguration.setProxyPort(proxyParams.get("proxy.port"));
		Launcher.applicationConfiguration.setProxyType(proxyParams.get("proxy.type"));
		Launcher.applicationConfiguration.setProxyUsername(proxyParams.get("proxy.username"));

		String userHome = System.getProperty("user.home");
		File f = new File(userHome + File.separator + ".syncany" + File.separator + "syncany-gui-config.xml");
		
		ApplicationConfigurationTO to = ApplicationConfiguration.toTO(Launcher.applicationConfiguration);
		try {
			ApplicationConfigurationTO.store(to, f);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		shell.dispose();
	}

	protected void handleTreeSelection() {
		if (tree.getSelectionCount() > 0){
			TreeItem ti = tree.getSelection()[0];
			if (ti == menuItemProxy){
				stackLayout.topControl = proxySettingsPanel;
				composite.layout();
			}
			else if (ti == menuItemGeneral){
				stackLayout.topControl = generalSettingsPanel;
				composite.layout();
			}
			else if (ti == menuItemAccount){
				stackLayout.topControl = accountSettingsPanel;
				composite.layout();
			}
			else if (ti == menuItemOptions){
				stackLayout.topControl = defaultPanel;
				composite.layout();
			}
		}
	}

	private Composite composite;
	private StackLayout stackLayout;
	private Tree tree;
	private TreeItem menuItemGeneral;
	private TreeItem menuItemProxy;
	private TreeItem menuItemAccount;
	private TreeItem menuItemOptions;
	
	private Composite defaultPanel;
	private Composite generalSettingsPanel;
	private ProxySettingsPanel proxySettingsPanel;
	private Composite accountSettingsPanel;
	
	private Label horizontalSeparatorLabel;
	private Label lblNewLabel_1;
	private Label optionsMessageTitleLabel;
	private Label optionsMessageLabel;
}
