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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.syncany.gui.CommonParameters;
import org.syncany.gui.UserInput;
import org.syncany.gui.WidgetDecorator;
import org.syncany.gui.panel.PluginPanel;
import org.syncany.gui.panel.PluginPanel.PluginPanelPurpose;
import org.syncany.gui.util.I18n;
import org.syncany.plugins.Plugin;
import org.syncany.plugins.Plugins;
import org.syncany.plugins.transfer.StorageTestResult;
import org.syncany.util.StringUtil;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class RepositorySelectionPanel extends WizardPanelComposite {
	private static final Logger log = Logger.getLogger(RepositorySelectionPanel.class.getSimpleName());
	
	private Composite pluginStackComposite;
	private Composite rootComposite;
	private Composite createComposite;
	private Composite urlComposite;

	private Text urlText;
	private Combo repositorySelectionCombo;
	private List<Plugin> pluginList;
	private StackLayout rootStackLayout;
	private StackLayout stackLayout;
	private String selectedPluginId;
	private Label chooseRepositoryLabel;
	private Label urlLabel;
	private Label urlIntroductionLabel;
	private Label urlIntroductionTitleLabel;
	
	private Map<String, PluginPanel> panels = new HashMap<>();
	private Label urlInvalidLabel;
	private Button testButton;
	private Label testResultLabel;
	private Composite composite_1;
	private Button btnCheckButton;
	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public RepositorySelectionPanel(WizardDialog wizardParentDialog, Composite parent, int style) {
		super(wizardParentDialog, parent, style);
		this.pluginList = Plugins.list();
		initComposite();
	}
	
	private void initComposite(){
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(1, false);
		gl_composite.marginRight = 30;
		setLayout(gl_composite);
		
		rootComposite = new Composite(this, SWT.NONE);
		rootStackLayout = new StackLayout();
		
		rootComposite.setLayout(rootStackLayout);
		rootComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		urlComposite = new Composite(rootComposite, SWT.NONE);
		urlComposite.setLayout(new GridLayout(1, false));
		
		urlIntroductionTitleLabel =new Label(urlComposite, SWT.WRAP);
		urlIntroductionTitleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		urlIntroductionTitleLabel.setText(I18n.getString("dialog.chooseRepository.url.introduction.title"));
		
		urlIntroductionLabel =new Label(urlComposite, SWT.WRAP);
		urlIntroductionLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		urlIntroductionLabel.setText(I18n.getString("dialog.chooseRepository.url.introduction"));
		
		urlLabel = new Label(urlComposite, SWT.NONE);
		GridData gd_urlLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_urlLabel.verticalIndent = WidgetDecorator.VERTICAL_INDENT;
		urlLabel.setLayoutData(gd_urlLabel);
		urlLabel.setText(I18n.getString("dialog.chooseRepository.url", true));
		
		createComposite = new Composite(rootComposite, SWT.NONE);
		createComposite.setLayout(new GridLayout(1, false));
		
		Label introductionTitleLabel =new Label(createComposite, SWT.NONE);
		introductionTitleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		introductionTitleLabel.setSize(262, 17);
		introductionTitleLabel.setText(I18n.getString("dialog.chooseRepository.introduction.title"));
		
		Label introductionLabel =new Label(createComposite, SWT.WRAP);
		introductionLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		introductionLabel.setSize(218, 17);
		introductionLabel.setText(I18n.getString("dialog.chooseRepository.introduction"));
		
		chooseRepositoryLabel =new Label(createComposite, SWT.NONE);
		chooseRepositoryLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		chooseRepositoryLabel.setSize(225, 17);
		chooseRepositoryLabel.setText(I18n.getString("dialog.chooseRepository.choosePlugin", true));
		
		repositorySelectionCombo = new Combo(createComposite, SWT.READ_ONLY);
		GridData gd_repositorySelectionCombo = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_repositorySelectionCombo.verticalIndent = WidgetDecorator.VERTICAL_INDENT;
		repositorySelectionCombo.setLayoutData(gd_repositorySelectionCombo);
		repositorySelectionCombo.setSize(387, 25);
		repositorySelectionCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String id = (String)repositorySelectionCombo.getData(repositorySelectionCombo.getItem(repositorySelectionCombo.getSelectionIndex()));
				showPLuginPanel(id);;
			}
		});
		
		pluginStackComposite = new Composite(createComposite, SWT.NONE);
		pluginStackComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		pluginStackComposite.setSize(242, 154);
		stackLayout = new StackLayout();
		pluginStackComposite.setLayout(stackLayout);
		
		for (Plugin p : pluginList){
			repositorySelectionCombo.add(p.getName());
			repositorySelectionCombo.setData(p.getName(), p.getId());
		}
		
		for (Plugin p : pluginList){
			String pluginPanelClassName = String.format("org.syncany.gui.plugin.%sPluginPanel", StringUtil.toCamelCase(p.getId()));
			
			try {
				Class<?>[] type = { Composite.class, int.class };
				Class<?> classDefinition = Class.forName(pluginPanelClassName);
				Constructor<?> cons = classDefinition.getConstructor(type);
				Object[] obj = { pluginStackComposite, SWT.NONE};

				PluginPanel pluginPanel = (PluginPanel) cons.newInstance(obj);
				panels.put(p.getId(), pluginPanel);
			}
			catch (Exception e) {
				log.warning("Unable to instanciate plugin gui panel " + pluginPanelClassName);
			}
		}
		int idxSelectedPlugin = 0;
		repositorySelectionCombo.select(idxSelectedPlugin);

		showPLuginPanel(pluginList.get(idxSelectedPlugin).getId());
		
		WidgetDecorator.bold(urlIntroductionTitleLabel, introductionTitleLabel);
		
		urlText = new Text(urlComposite, SWT.BORDER | SWT.WRAP | SWT.MULTI);
		GridData gd_urlText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_urlText.heightHint = 95;
		urlText.setLayoutData(gd_urlText);
		WidgetDecorator.normal(
			urlIntroductionLabel, introductionLabel, 
			chooseRepositoryLabel, 
			urlText, urlLabel
		);
		
		composite_1 = new Composite(createComposite, SWT.NONE);
		composite_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		composite_1.setLayout(new GridLayout(2, false));
		
		btnCheckButton = new Button(composite_1, SWT.CHECK);
		btnCheckButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		btnCheckButton.setText("Create repository if required");
		
		testResultLabel = new Label(composite_1, SWT.WRAP);
		testResultLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		testResultLabel.setSize(55, 15);
		
		testButton = new Button(composite_1, SWT.NONE);
		
		GridData gd_testFtpButton = new GridData(SWT.CENTER, SWT.FILL, false, false, 1, 1);
		gd_testFtpButton.heightHint = WidgetDecorator.DEFAULT_BUTTON_HEIGHT;
		gd_testFtpButton.widthHint = WidgetDecorator.DEFAULT_BUTTON_WIDTH;
		testButton.setLayoutData(gd_testFtpButton);
		
		testButton.setSize(75, 25);
		testButton.setText(I18n.getString("plugin.test"));
		testButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleTest();
			}
		});
		
		urlInvalidLabel = new Label(urlComposite, SWT.NONE);
		urlInvalidLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
	}
	
	private void showPLuginPanel(String id){
		this.selectedPluginId = id;
		PluginPanel ppanel = panels.get(id);
		stackLayout.topControl = ppanel;
		pluginStackComposite.layout();
		createComposite.layout();
		String action = getParentWizardDialog().getUserInput().getCommonParameter(CommonParameters.COMMAND_ACTION);
		
		if (action != null && ppanel != null){
			PluginPanelPurpose purpose = PluginPanelPurpose.valueOf(action.toUpperCase());
			ppanel.setPurpose(purpose);
		}
	}
	
	private void handleTest() {
		PluginPanel ppanel = panels.get(selectedPluginId);
		
		testButton.setEnabled(false);
		try{
			boolean isValid = isValid();
			StorageTestResult testResult = ppanel.testPluginConnection();
			String tempMessage = "message";
			Color tempColor = WidgetDecorator.BLACK;
			
			if (isValid){
	    		if (testResult.isRepoFileExists()) {
	    			
	    		}
	    		
	    	}
	    	else {
	    		tempMessage = I18n.getString("plugin.storageTestResult.notValid");
	    		tempColor = WidgetDecorator.RED;
	    	}
	    	
	    	final String message = tempMessage;
	    	final Color color = tempColor;
	    	
			Display.getCurrent().syncExec(new Runnable() {
			    public void run() {
			    	testResultLabel.setText(message);
			    	testResultLabel.setForeground(color);
			    	testButton.setEnabled(true);
			    }
			});
		}
		catch (Exception e){
			
		}
		finally {
			testButton.setEnabled(true);
		}
	}

	@Override
	public boolean isValid() {
		boolean urlAvailable = getParentWizardDialog().getUserInput().getCommonParameterAsBoolean(CommonParameters.AVAILABLE_URL);
		boolean valid = true;
		
		if (urlAvailable){
			Pattern LINK_PATTERN = Pattern.compile("^syncany://storage/1/(?:(not-encrypted/)(.+)|([^-]+-(.+)))$");
			
			Matcher linkMatcher = LINK_PATTERN.matcher(urlText.getText());
			
			if (urlText.getText() == null || urlText.getText().length() == 0 || !linkMatcher.matches()) {
				urlText.setBackground(WidgetDecorator.INVALID_TEXT_COLOR);
				urlInvalidLabel.setText("Invalid URL");
				valid = false;
			} 
			else {
				urlText.setBackground(WidgetDecorator.WHITE);
				urlInvalidLabel.setText("");
			}
		}
		else{
			PluginPanel ppanel = panels.get(selectedPluginId);
			valid = ppanel.isValid();
		}
		
		return valid;
	}

	@Override
	public UserInput getUserSelection() {
		UserInput userInput = new UserInput();
		boolean urlAvailable = getParentWizardDialog().getUserInput().getCommonParameterAsBoolean(CommonParameters.AVAILABLE_URL);
		
		if (urlAvailable){
			userInput.putCommonParameter(CommonParameters.URL, urlText.getText());
		}
		else{
			String id = (String)repositorySelectionCombo.getData(repositorySelectionCombo.getItem(repositorySelectionCombo.getSelectionIndex()));
			userInput.putCommonParameter(CommonParameters.PLUGIN_ID, id);
			UserInput pluginParameters = panels.get(id).getUserSelection();
			userInput.merge(pluginParameters);
		}
		return userInput;
	}

	@Override
	public boolean hasNextButton() {
		return true;
	}

	@Override
	public boolean hasPreviousButton() {
		return true;
	}

	@Override
	public boolean hasFinishButton() {
		return false;
	}

	@Override
	public void updateData() {
		String action = getParentWizardDialog().getUserInput().getCommonParameter(CommonParameters.COMMAND_ACTION);
		
		boolean url = getParentWizardDialog().getUserInput().getCommonParameterAsBoolean(CommonParameters.AVAILABLE_URL);
		
		if (action.equals("create")  || (action.equals("connect") && !url)){
			rootStackLayout.topControl = createComposite;
		}
		else{
			rootStackLayout.topControl = urlComposite;
		}
		rootComposite.layout();
	}
}
