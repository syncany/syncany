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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.gui.ApplicationResourcesManager;
import org.syncany.gui.panel.PluginPanel;
import org.syncany.util.I18n;
import org.syncany.util.StringUtil;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class RepositorySelectionPanel extends WizardPanelComposite {
	private static final Logger log = Logger.getLogger(RepositorySelectionPanel.class.getSimpleName());
	
	private Combo repositorySelectionCombo;
	private List<Plugin> pluginList;
	private Composite pluginStackComposite;
	
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
		Font fontNormal = ApplicationResourcesManager.FONT_NORMAL;
		Font fontBold = ApplicationResourcesManager.FONT_BOLD;
		
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(2, false);
		setLayout(gl_composite);
		
		Label introductionTitleLabel = new Label(this, SWT.NONE);
		introductionTitleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		introductionTitleLabel.setFont(fontBold);
		introductionTitleLabel.setText(I18n.getString("dialog.chooseRepository.introduction.title"));
		
		Label introductionLabel = new Label(this, SWT.WRAP);
		introductionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
		introductionLabel.setFont(fontNormal);
		introductionLabel.setText(I18n.getString("dialog.chooseRepository.introduction"));
		
		chooseRepositoryLabel = new Label(this, SWT.NONE);
		GridData gd_chooseRepositoryLabel = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_chooseRepositoryLabel.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		chooseRepositoryLabel.setLayoutData(gd_chooseRepositoryLabel);
		chooseRepositoryLabel.setFont(fontNormal);
		chooseRepositoryLabel.setText(I18n.getString("dialog.chooseRepository.choosePlugin", true));
		
		repositorySelectionCombo = new Combo(this, SWT.READ_ONLY);
		repositorySelectionCombo.setFont(fontNormal);
		GridData gd_repositorySelectionCombo = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_repositorySelectionCombo.verticalIndent = ApplicationResourcesManager.VERTICAL_INDENT;
		repositorySelectionCombo.setLayoutData(gd_repositorySelectionCombo);
		repositorySelectionCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String id = (String)repositorySelectionCombo.getData(repositorySelectionCombo.getItem(repositorySelectionCombo.getSelectionIndex()));
				showPLuginPanel(id);;
			}
		});
		
		for (Plugin p : pluginList){
			repositorySelectionCombo.add(p.getName());
			repositorySelectionCombo.setData(p.getName(), p.getId());
		}
		
		pluginStackComposite = new Composite(this, SWT.NONE);
		GridData gd_pluginStackComposite = new GridData(SWT.LEFT, SWT.FILL, false, true, 2, 1);
		gd_pluginStackComposite.verticalIndent = 30;
		gd_pluginStackComposite.horizontalIndent = 30;
		pluginStackComposite.setLayoutData(gd_pluginStackComposite);
		stackLayout = new StackLayout();
		pluginStackComposite.setLayout(stackLayout);
		
		for (Plugin p : pluginList){
			String pluginPanelClassName = String.format("org.syncany.gui.plugin.%sPluginPanel", StringUtil.toCamelCase(p.getId()));
			
			try {
				Class<?>[] type = { Composite.class, int.class };
				Class<?> classDefinition = Class.forName(pluginPanelClassName);
				Constructor<?> cons = classDefinition.getConstructor(type);
				Object[] obj = { pluginStackComposite, SWT.NONE };
				
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
	}
	
	private void showPLuginPanel(String id){
		this.selectedPluginId = id;
		PluginPanel ppanel = panels.get(id);
		stackLayout.topControl = ppanel;
		pluginStackComposite.layout();
	}

	private String selectedPluginId;
	private Map<String, PluginPanel> panels = new HashMap<>();
	private StackLayout stackLayout;
	private Label chooseRepositoryLabel;
	
	@Override
	public boolean isValid() {
		PluginPanel ppanel = panels.get(selectedPluginId);
		return ppanel.isValid(); 
	}

	@Override
	public Map<String, String> getUserSelection() {
		String id = (String)repositorySelectionCombo.getData(repositorySelectionCombo.getItem(repositorySelectionCombo.getSelectionIndex()));Map<String, String> userInput = new HashMap<>();
		userInput.put("repositoryType", id);
		Map<String, String> pluginParameters = panels.get(id).getUserSelection();
		userInput.putAll(pluginParameters);
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
		// TODO Auto-generated method stub
		
	}
}
