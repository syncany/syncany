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
	private Text accessKey;
	private Text secretKey;
	private Text bucket;
	private Text location;

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
		
		Label introductionTitleLabel = WidgetDecorator.label(this, SWT.WRAP).bold();
		introductionTitleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		introductionTitleLabel.setText(I18n.getString("plugin.s3.introduction.title"));
		
		Label introductionLabel =  WidgetDecorator.label(this, SWT.WRAP).normal();
		introductionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
		introductionLabel.setText(I18n.getString("plugin.s3.introduction"));
		
		Label accessKeyLabel =  WidgetDecorator.label(this, SWT.NONE).normal();
		GridData gd_accessKeyLabel = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_accessKeyLabel.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		accessKeyLabel.setLayoutData(gd_accessKeyLabel);
		accessKeyLabel.setText(I18n.getString("plugin.s3.accessKey", true));
		
		accessKey = WidgetDecorator.text(this, SWT.BORDER).normal();
		GridData gd_hostText = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_hostText.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		gd_hostText.minimumWidth = 200;
		accessKey.setLayoutData(gd_hostText);
		
		Label secretKeyLabel =  WidgetDecorator.label(this, SWT.NONE).normal();
		secretKeyLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		secretKeyLabel.setText(I18n.getString("plugin.s3.secretKey", true));
		
		secretKey =  WidgetDecorator.text(this, SWT.BORDER).normal();
		secretKey.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		Label bucketLabel =  WidgetDecorator.label(this, SWT.NONE).normal();
		bucketLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		bucketLabel.setText(I18n.getString("plugin.s3.bucket", true));
		
		bucket = WidgetDecorator.text(this, SWT.BORDER).normal();
		bucket.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		Label locationLabel =  WidgetDecorator.label(this, SWT.NONE).normal();
		locationLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		locationLabel.setText(I18n.getString("plugin.s3.location", true));
		
		location = WidgetDecorator.text(this, SWT.BORDER).normal();
		location.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
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
		
		final Button testButton = WidgetDecorator.button(buttonComposite, SWT.NONE).normal();

		GridData gd_testButton = new GridData(SWT.CENTER, SWT.FILL, false, false, 1, 1);
		gd_testButton.heightHint = 30;
		gd_testButton.widthHint = 100;
		testButton.setLayoutData(gd_testButton);
		testButton.setText(I18n.getString("plugin.s3.testConnection"));
	}

	@Override
	public UserInput getUserSelection() {
		UserInput parameters = new UserInput();
		parameters.put(SyncanyRestParameters.ACCESS_KEY, accessKey.getText());
		parameters.put(SyncanyRestParameters.SECRET_KEY, secretKey.getText());
		parameters.put(SyncanyRestParameters.BUCKET, bucket.getText());
		parameters.put(SyncanyRestParameters.LOCATION, location.getText());
		return parameters;
	}

	@Override
	public boolean isValid() {
		boolean valid = true;
		
		// && order matters cause java uses lazy evaluation
		valid = SWTUtil.checkTextLength(accessKey, 0) && valid;
		valid = SWTUtil.checkTextLength(secretKey, 0) && valid;
		valid = SWTUtil.checkTextLength(bucket, 0) && valid;
		valid = SWTUtil.checkTextLength(location, 0) && valid;
			
		return valid;
	}
}
