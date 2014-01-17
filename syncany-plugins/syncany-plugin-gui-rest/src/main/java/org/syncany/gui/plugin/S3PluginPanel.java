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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.PluginOptionSpec.OptionValidationResult;
import org.syncany.connection.plugins.PluginOptionSpecs;
import org.syncany.connection.plugins.Plugins;
import org.syncany.gui.ApplicationResourcesManager;
import org.syncany.gui.SWTUtil;
import org.syncany.gui.UserInput;
import org.syncany.gui.WidgetDecorator;
import org.syncany.gui.panel.PluginPanel;
import org.syncany.util.I18n;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 * 
 */
public class S3PluginPanel extends PluginPanel {
	private Text accessKeyText;
	private Text secretKeyText;
	private Text bucketText;
	private Text locationText;

	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public S3PluginPanel(Composite parent, int style){
		super(parent, style);
		initComposite();
	}
	
	public void initComposite(){
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(2, false);
		setLayout(gl_composite);

		Label introductionTitleLabel = new Label(this, SWT.WRAP);
		introductionTitleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		introductionTitleLabel.setText(I18n.getString("plugin.s3.introduction.title"));
		
		Label introductionLabel =  new Label(this, SWT.WRAP);
		introductionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
		introductionLabel.setText(I18n.getString("plugin.s3.introduction"));
		
		Label accessKeyLabel =  new Label(this, SWT.NONE);
		GridData gd_accessKeyLabel = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_accessKeyLabel.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		accessKeyLabel.setLayoutData(gd_accessKeyLabel);
		accessKeyLabel.setText(I18n.getString("plugin.s3.accessKey", true));
		
		accessKeyText = new Text(this, SWT.BORDER);
		GridData gd_hostText = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_hostText.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		gd_hostText.minimumWidth = 200;
		accessKeyText.setLayoutData(gd_hostText);
		
		Label secretKeyLabel =  new Label(this, SWT.NONE);
		secretKeyLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		secretKeyLabel.setText(I18n.getString("plugin.s3.secretKey", true));
		
		secretKeyText =  new Text(this, SWT.BORDER);
		secretKeyText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		Label bucketLabel =  new Label(this, SWT.NONE);
		bucketLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		bucketLabel.setText(I18n.getString("plugin.s3.bucket", true));
		
		bucketText = new Text(this, SWT.BORDER);
		bucketText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		Label locationLabel =  new Label(this, SWT.NONE);
		locationLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		locationLabel.setText(I18n.getString("plugin.s3.location", true));
		
		locationText = new Text(this, SWT.BORDER);
		locationText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		Composite buttonComposite = new Composite(this, SWT.NONE);
		GridLayout gl_buttonComposite = new GridLayout(2, false);
		gl_buttonComposite.horizontalSpacing = 0;
		gl_buttonComposite.verticalSpacing = 0;
		gl_buttonComposite.marginWidth = 0;
		gl_buttonComposite.marginHeight = 0;
		buttonComposite.setLayout(gl_buttonComposite);
		GridData gd_buttonComposite = new GridData(SWT.RIGHT, SWT.BOTTOM, false, false, 4, 1);
		gd_buttonComposite.minimumHeight = 30;
		buttonComposite.setLayoutData(gd_buttonComposite);
		
		final Label testResultLabel = new Label(buttonComposite, SWT.NONE);
		testResultLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		testResultLabel.setAlignment(SWT.CENTER);
		
		final Button testButton = new Button(buttonComposite, SWT.NONE);

		GridData gd_testButton = new GridData(SWT.CENTER, SWT.FILL, false, false, 1, 1);
		gd_testButton.heightHint = 30;
		gd_testButton.widthHint = 100;
		testButton.setLayoutData(gd_testButton);
		testButton.setText(I18n.getString("plugin.s3.testConnection"));
		
		WidgetDecorator.bold(introductionTitleLabel);
		WidgetDecorator.normal(
			introductionLabel, 
			accessKeyLabel, accessKeyText, 
			secretKeyLabel, secretKeyText, 
			bucketLabel, bucketText, 
			locationLabel, locationText
		);
	}

	@Override
	public UserInput getUserSelection() {
		UserInput parameters = new UserInput();
		parameters.putPluginParameter("accessKey", accessKeyText.getText());
		parameters.putPluginParameter("secretKey", secretKeyText.getText());
		parameters.putPluginParameter("bucket", bucketText.getText());
		parameters.putPluginParameter("location", locationText.getText());
		return parameters;
	}

	@Override
	public boolean isValid() {
		boolean valid = true;

		Plugin plugin = Plugins.get("ftp");
		Connection c = plugin.createConnection();

		PluginOptionSpecs poc = c.getOptionSpecs();
		
		OptionValidationResult res;
		
		res = poc.get("accessKey").validateInput(accessKeyText.getText());
		SWTUtil.markAs(res.equals(OptionValidationResult.VALID), accessKeyText);
		if (!res.equals(OptionValidationResult.VALID)){
			valid = false;
		}
		
		res = poc.get("secretKey").validateInput(secretKeyText.getText());
		SWTUtil.markAs(res.equals(OptionValidationResult.VALID), secretKeyText);
		if (!res.equals(OptionValidationResult.VALID)){
			valid = false;
		}
		
		res = poc.get("bucket").validateInput(bucketText.getText());
		SWTUtil.markAs(res.equals(OptionValidationResult.VALID), bucketText);
		if (!res.equals(OptionValidationResult.VALID)){
			valid = false;
		}
		
		res = poc.get("location").validateInput(locationText.getText());
		SWTUtil.markAs(res.equals(OptionValidationResult.VALID), locationText);
		if (!res.equals(OptionValidationResult.VALID)){
			valid = false;
		}
		
		return valid;
	}
}
