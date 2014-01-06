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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.syncany.gui.ApplicationResourcesManager;
import org.syncany.gui.panel.PluginPanel;
import org.syncany.util.I18n;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 * 
 */
public class S3PluginPanel extends PluginPanel {
	private static final Logger log = Logger.getLogger(S3PluginPanel.class.getSimpleName());
	
	private Text accessKey;
	private Text secretKey;
	private Text bucket;
	private Text location;

	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public S3PluginPanel(Composite parent, int style) {
		super(parent, style);
		
		initComposite();
	}
	
	public void initComposite(){
		Font fontNormal = ApplicationResourcesManager.FONT_NORMAL;
		Font fontBold = ApplicationResourcesManager.FONT_BOLD;
		
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(2, false);
		setLayout(gl_composite);
		
		Label introductionTitleLabel = new Label(this, SWT.WRAP);
		introductionTitleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		introductionTitleLabel.setText(I18n.getString("plugin.s3.introduction.title"));
		introductionTitleLabel.setFont(fontBold);
		
		Label introductionLabel = new Label(this, SWT.WRAP);
		introductionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
		introductionLabel.setText(I18n.getString("plugin.s3.introduction"));
		introductionLabel.setFont(fontNormal);
		
		Label accessKeyLabel = new Label(this, SWT.NONE);
		GridData gd_accessKeyLabel = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_accessKeyLabel.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		accessKeyLabel.setLayoutData(gd_accessKeyLabel);
		accessKeyLabel.setText(I18n.getString("plugin.s3.accessKey", true));
		accessKeyLabel.setFont(fontNormal);
		
		accessKey = new Text(this, SWT.BORDER);
		accessKey.setFont(fontNormal);
		GridData gd_hostText = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_hostText.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		gd_hostText.minimumWidth = 200;
		accessKey.setLayoutData(gd_hostText);
		
		Label secretKeyLabel = new Label(this, SWT.NONE);
		secretKeyLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		secretKeyLabel.setText(I18n.getString("plugin.s3.secretKey", true));
		secretKeyLabel.setFont(fontNormal);
		
		secretKey = new Text(this, SWT.BORDER);
		secretKey.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		secretKey.setFont(fontNormal);
		
		Label bucketLabel = new Label(this, SWT.NONE);
		bucketLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		bucketLabel.setText(I18n.getString("plugin.s3.bucket", true));
		bucketLabel.setFont(fontNormal);
		
		bucket = new Text(this, SWT.BORDER);
		bucket.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		bucket.setFont(fontNormal);
		
		Label locationLabel = new Label(this, SWT.NONE);
		locationLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		locationLabel.setText(I18n.getString("plugin.s3.location", true));
		locationLabel.setFont(fontNormal);
		
		location = new Text(this, SWT.BORDER);
		location.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		location.setFont(fontNormal);
		
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
		testButton.setFont(fontNormal);
		testButton.setText(I18n.getString("plugin.s3.testConnection"));
	}

	@Override
	public Map<String, String> getUserSelection() {
		Map<String, String> parameters = new HashMap<>();
		parameters.put("plugin.s3.accessKey", accessKey.getText());
		parameters.put("plugin.s3.secretKey", secretKey.getText());
		parameters.put("plugin.s3.bucket", bucket.getText());
		parameters.put("plugin.s3.location", location.getText());
		return parameters;
	}

	@Override
	public boolean isValid() {
		return true;
	}
}
