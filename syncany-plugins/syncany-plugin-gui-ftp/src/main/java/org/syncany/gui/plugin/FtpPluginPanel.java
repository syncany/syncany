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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.PluginOptionSpec.OptionValidationResult;
import org.syncany.connection.plugins.PluginOptionSpecs;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.TransferManager.StorageTestResult;
import org.syncany.gui.UserInput;
import org.syncany.gui.WidgetDecorator;
import org.syncany.gui.panel.PluginPanel;
import org.syncany.util.I18n;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class FtpPluginPanel extends PluginPanel {
	private Text hostText;
	private Text usernameText;
	private Text passwordText;
	private Text pathText;
	private Button testFtpButton;
	private Label testResultLabel;
	private Spinner portSpinner;

	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public FtpPluginPanel(Composite parent, int style) {
		super(parent, style);
		initComposite();
	}
	
	public void initComposite(){
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(4, false);
		setLayout(gl_composite);
		
		Label introductionTitleLabel = new Label(this, SWT.WRAP);
		introductionTitleLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		introductionTitleLabel.setText(I18n.getString("plugin.ftp.introduction.title"));
		
		Label introductionLabel = new Label(this, SWT.WRAP);
		introductionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false, 4, 1));
		introductionLabel.setText(I18n.getString("plugin.ftp.introduction"));
		
		Label hostLabel = new Label(this, SWT.NONE);
		GridData gd_hostLabel = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_hostLabel.verticalIndent = WidgetDecorator.VERTICAL_INDENT;
		hostLabel.setLayoutData(gd_hostLabel);
		hostLabel.setText(I18n.getString("plugin.ftp.host", true));
		
		hostText = new Text(this, SWT.BORDER);
		
		GridData gd_hostText = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_hostText.verticalIndent = WidgetDecorator.VERTICAL_INDENT;
		hostText.setLayoutData(gd_hostText);
		
		Label portLabel = new Label(this, SWT.NONE);
		GridData gd_portLabel = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_portLabel.verticalIndent = WidgetDecorator.VERTICAL_INDENT;
		portLabel.setLayoutData(gd_portLabel);
		portLabel.setText(I18n.getString("plugin.ftp.port", true));
		
		portSpinner = new Spinner(this, SWT.BORDER);
		portSpinner.setPageIncrement(1);
		portSpinner.setMaximum(100000);
//		WidgetDecorator.decorateControl(spinner, FontDecorator.NORMAL);
		portSpinner.setSelection(21);
		GridData gd_spinner = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_spinner.verticalIndent = WidgetDecorator.VERTICAL_INDENT;
		gd_spinner.widthHint = 50;
		gd_spinner.heightHint = 15;
		portSpinner.setLayoutData(gd_spinner);
		
		Label usernameLabel = new Label(this, SWT.NONE);
		usernameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		usernameLabel.setText(I18n.getString("plugin.ftp.username", true));
		
		usernameText = new Text(this, SWT.BORDER);
		usernameText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		
		Label passwordLabel = new Label(this, SWT.NONE);
		passwordLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		passwordLabel.setText(I18n.getString("plugin.ftp.password", true));
		
		passwordText = new Text(this, SWT.BORDER | SWT.PASSWORD);
		passwordText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		
		Label pathLabel = new Label(this, SWT.NONE);
		pathLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		pathLabel.setText(I18n.getString("plugin.ftp.path", true));
		
		pathText = new Text(this, SWT.BORDER);
		pathText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));

		Composite buttonComposite = new Composite(this, SWT.NONE);
		GridLayout gl_buttonComposite = new GridLayout(2, false);
		gl_buttonComposite.horizontalSpacing = 0;
		gl_buttonComposite.verticalSpacing = 0;
		gl_buttonComposite.marginWidth = 0;
		gl_buttonComposite.marginHeight = 0;
		buttonComposite.setLayout(gl_buttonComposite);
		GridData gd_buttonComposite = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		gd_buttonComposite.minimumHeight = 30;
		buttonComposite.setLayoutData(gd_buttonComposite);
		
		testResultLabel = new Label(buttonComposite, SWT.WRAP);
		testResultLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		testResultLabel.setAlignment(SWT.CENTER);
		
		testFtpButton = new Button(buttonComposite, SWT.NONE);

		GridData gd_testFtpButton = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_testFtpButton.heightHint = 30;
		gd_testFtpButton.widthHint = 100;
		testFtpButton.setLayoutData(gd_testFtpButton);
		testFtpButton.setText(I18n.getString("plugin.ftp.test"));
		testFtpButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleTest();
			}
		});
		
		WidgetDecorator.bold(introductionTitleLabel);
		WidgetDecorator.normal(
			introductionLabel,
			hostText,hostLabel,
			portLabel,portSpinner, 
			usernameLabel, usernameText,
			passwordLabel,passwordText,
			pathLabel,pathText,
			testFtpButton
		);
	}

	protected void handleTest() {
		testFtpButton.setEnabled(false);
		try{
			boolean isValid = isValid();
			StorageTestResult testResult = testPluginConnection();
			String tempMessage = "message";
			Color tempColor = WidgetDecorator.BLACK;
			
			if (isValid){
	    		switch (testResult){
		    		case INVALID_PARAMETERS:
		    			tempMessage = I18n.getString("plugin.ftp.testFails");
		    			tempColor = WidgetDecorator.RED;
		    			break;
		    		case NO_REPO_LOCATION_EMPTY_PERMISSIONS_KO:
		    			tempMessage = I18n.getString("plugin.noRepoLocationEmptyPermissionOK");
		    			tempColor = WidgetDecorator.RED;
		    			break;
		    		case NO_REPO_LOCATION_EMPTY_PERMISSIONS_OK:
		    			tempMessage = I18n.getString(
		    				"plugin.ftp.testSucceed", 
		    				I18n.getString("plugin.noRepoLocationEmptyPermissionOK")
		    			);
		    			break;
		    		case NO_REPO_LOCATION_NOT_EMPTY:
		    			tempMessage = I18n.getString("plugin.noRepoLocationNotEmpty");
		    			tempColor = WidgetDecorator.RED;
		    			break;
		    		case NO_REPO_PERMISSIONS_KO:
		    			tempMessage = I18n.getString("plugin.noRepoPermissionKO");
		    			tempColor = WidgetDecorator.RED;
		    			break;
		    		case NO_REPO_PERMISSIONS_OK:
		    			tempMessage = I18n.getString(
		    				"plugin.ftp.testSucceed", 
		    				I18n.getString("plugin.noRepoPermissionOK")
		    			);
		    			break;
		    		case REPO_ALREADY_EXISTS:
		    			tempMessage = I18n.getString("plugin.repoAlreadyExists");
		    			tempColor = WidgetDecorator.RED;
		    			break;
	    		}
	    	}
	    	else {
	    		tempMessage = I18n.getString("plugin.ftp.testFails");
	    		tempColor = WidgetDecorator.RED;
	    	}
	    	
	    	final String message = tempMessage;
	    	final Color color = tempColor;
	    	
			Display.getCurrent().syncExec(new Runnable() {
			    public void run() {
			    	testResultLabel.setText(message);
			    	testResultLabel.setForeground(color);
			    	testFtpButton.setEnabled(true);
			    }
			});
		}
		catch (Exception e){
			
		}
		finally {
			testFtpButton.setEnabled(true);
		}
	}

	@Override
	public UserInput getUserSelection() {
		UserInput parameters = new UserInput();
		parameters.putPluginParameter("hostname", hostText.getText());
		parameters.putPluginParameter("username", usernameText.getText());
		parameters.putPluginParameter("password", passwordText.getText());
		parameters.putPluginParameter("path", pathText.getText());
		parameters.putPluginParameter("port", portSpinner.getText());
		return parameters;
	}
	
	@Override
	public boolean isValid() {
		boolean valid = true;

		Plugin plugin = Plugins.get("ftp");
		Connection c = plugin.createConnection();

		PluginOptionSpecs poc = c.getOptionSpecs();
		
		OptionValidationResult res;
		
		res = poc.get("hostname").validateInput(hostText.getText());
		WidgetDecorator.markAs(res.equals(OptionValidationResult.VALID), hostText);
		if (!res.equals(OptionValidationResult.VALID)){
			valid = false;
		}
		
		res = poc.get("username").validateInput(usernameText.getText());
		WidgetDecorator.markAs(res.equals(OptionValidationResult.VALID), usernameText);
		if (!res.equals(OptionValidationResult.VALID)){
			valid = false;
		}
		
		res = poc.get("password").validateInput(passwordText.getText());
		WidgetDecorator.markAs(res.equals(OptionValidationResult.VALID), passwordText);
		if (!res.equals(OptionValidationResult.VALID)){
			valid = false;
		}
		
		res = poc.get("path").validateInput(pathText.getText());
		WidgetDecorator.markAs(res.equals(OptionValidationResult.VALID), pathText);
		if (!res.equals(OptionValidationResult.VALID)){
			valid = false;
		}
		
		res = poc.get("port").validateInput(portSpinner.getText());
		WidgetDecorator.markAs(res.equals(OptionValidationResult.VALID), portSpinner);
		if (!res.equals(OptionValidationResult.VALID)){
			valid = false;
		}
		
		return valid;
	}

	@Override
	public String getPluginId() {
		return "ftp";
	}
}
